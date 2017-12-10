from py4j.java_gateway import java_import, get_field
from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
from functools import wraps
import uuid
import time
import threading
import sys
import atexit

_async_registry_lock = threading.Lock()
_async_registry = {}

gateway = ClientServer(
    java_parameters=JavaParameters(auto_field=True), 
    python_parameters=PythonParameters(daemonize=True))

mc = gateway.new_jvm_view()

java_import(gateway.jvm, 'com.macuyiko.minecraftpyserver.py4j.*')
java_import(mc, 'org.bukkit.*')
java_import(mc, 'org.bukkit.event.*')
java_import(mc, 'org.bukkit.command.*')

BUKKIT = mc.Bukkit
SERVER = BUKKIT.getServer()
WORLD  = SERVER.getWorlds().get(0)
PLUGIN = SERVER.getPluginManager().getPlugin('MinecraftPyServer')

class AttrWrapper(object):
    def __init__(self, wrapped):
        self._wrapped = wrapped
        self.delay = None
    def __str__(self):
        return self._wrapped.__str__()
    def __getattr__(self, name):
        f = getattr(self._wrapped, name)
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            run_spigot_thread(g, self.delay)
        self.delay = None
        return wrapped_f
    
class AsyncTask(object):
    def __init__(self):
        self.id = uuid.uuid4()
        _async_registry_lock.acquire()
        try:
            _async_registry[self.id] = self
        finally:
            _async_registry_lock.release()
    def remove(self):
        _async_registry_lock.acquire()
        try:
            del _async_registry[self.id]
        finally:
            _async_registry_lock.release()

class PythonCallbackImpl(AsyncTask):
    def __init__(self, execfunc):
        super().__init__()
        self.execfunc = execfunc
    def callback(self, *args):
        try:
            self.execfunc(*args)
        except Exception as e:
            traceback.print_exc()
        finally:
            self.remove()
        sys.exit()
    class Java:
        implements = ["com.macuyiko.minecraftpyserver.py4j.PyCallback"]

def shutdown_when_done():
    tries = 0
    amount = 1
    while amount > 0:
        time.sleep(5)
        tries += 1
        if tries > 10:
            print('Giving up... you should close Python manually')
        _async_registry_lock.acquire()
        try:
            amount = len(_async_registry)
        finally:
            _async_registry_lock.release()
        print('Waiting for callbacks...', amount, 'left')
        for thread in threading.enumerate():
            print(thread.name, thread.daemon)
    print('All done!')

def run_local_thread(execfunc):
    threading.Thread(target=execfunc).start()

def run_spigot_thread(execfunc, delay=None):
    python_callback = PythonCallbackImpl(execfunc)
    spigot_runnable = gateway.jvm.SpigotRunnable(python_callback)
    if delay is None: spigot_runnable.runTask(PLUGIN)
    else: spigot_runnable.runTaskLater(PLUGIN, delay)

def asynchronous():
    def actual_decorator(f):
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            run_local_thread(g)
        return wrapped_f
    return actual_decorator

def synchronous(delay=None):
    def actual_decorator(f):
        @wraps(f)
        def wrapped_f(*args, **kwargs):
            g = lambda: f(*args, **kwargs)
            run_spigot_thread(g, delay)
        return wrapped_f
    return actual_decorator

def register_command(name, execfunc):
    python_callback = PythonCallbackImpl(execfunc)
    spigot_command = gateway.jvm.SpigotCommand(name, python_callback)
    return spigot_command

def register_event(event_type, execfunc):
    python_callback = PythonCallbackImpl(execfunc)
    spigot_listener = gateway.jvm.SpigotEventListener(event_type, python_callback)
    return spigot_listener

def parseargswithpos(args, kwargs, asint=True, ledger={}):
    results = {}
    if kwargs is None: kwargs = {}
    if isinstance(args[0], int) or isinstance(args[0], float):
        base = 3
        tr = [args[0], args[1], args[2]]
    elif 'x' in kwargs and 'y' in kwargs and 'z' in kwargs:
        base = 0
        tr = [kwargs['x'], kwargs['y'], kwargs['z']]
    elif isinstance(args[0], list):
        base = 1
        tr = [args[0][0], args[0][1], args[0][2]]
    elif isinstance(args[0], dict):
        base = 1
        tr = [args[0]['x'], args[0]['y'], args[0]['z']]
    else:
        base = 1
        tr = [args[0].x, args[0].y, args[0].z]
    if asint:
        pos = (int(tr[0]), int(tr[1]), int(tr[2]))
    results['x'] = pos[0]
    results['y'] = pos[1]
    results['z'] = pos[2]
    for k,v in ledger.iteritems():
        results[k] = kwargs.get(v[0], None)
        if results[k] is None:
            if len(args) > base+v[1]:
                results[k] = args[base+v[1]]
            else:
                results[k] = v[2]
    return results

def location_for(*args):
    if len(args) == 1:
        return args[0].getLocation()
    return mc.Location(WORLD, *args)

world = AttrWrapper(WORLD)

atexit.register(shutdown_when_done)