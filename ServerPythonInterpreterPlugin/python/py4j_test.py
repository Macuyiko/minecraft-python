from py4j.java_gateway import JavaGateway, java_import, get_field, CallbackServerParameters, GatewayParameters
from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import uuid
import time
import threading
import sys

_async_registry_lock = threading.Lock()
_async_registry = {}
    
gateway = ClientServer(
    java_parameters=JavaParameters(auto_field=True), 
    python_parameters=PythonParameters(daemonize=True))

def shutdown_when_done():
    amount = 1
    while amount > 0:
        _async_registry_lock.acquire()
        try:
            amount = len(_async_registry)
        finally:
            _async_registry_lock.release()
        time.sleep(5)
        print('Waiting for callbacks...', amount, 'left')
    print('All done! Shutting down connection')
    gateway.close()
    
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
    def callback(self):
        ret = True
        try:
            self.execfunc()
        except:
            ret = False
        finally:
            self.remove()
            return ret
    class Java:
        implements = ["com.macuyiko.minecraftpyserver.py4j.PyCallback"]

# ------------------------------------------------------

java_import(gateway.jvm, 'org.bukkit.*')
java_import(gateway.jvm, 'com.macuyiko.minecraftpyserver.py4j.*')

BUKKIT = gateway.jvm.Bukkit
SERVER = BUKKIT.getServer()
WORLD = SERVER.getWorlds().get(0)
PLUGIN = SERVER.getPluginManager().getPlugin('MinecraftPyServer')

p = SERVER.getPlayer("Macuyiko")
print(p.getLocation())
print(BUKKIT)

def run_synchronous(execfunc, delay=None):
    python_callback = PythonCallbackImpl(execfunc)
    spigot_runnable = gateway.jvm.SpigotRunnable(python_callback)
    print(spigot_runnable)
    if delay is None: spigot_runnable.runTask(PLUGIN)
    else: spigot_runnable.runTaskLater(PLUGIN, delay)

def fun():
    print('calling fun')
    gateway.jvm.System.out.println("Hello from python!")
    p = SERVER.getPlayer("Macuyiko")
    l = p.getLocation()
    WORLD.createExplosion(l.getX(), l.getY(), l.getZ(), 3.0, True)

run_synchronous(fun, None)

shutdown_when_done()