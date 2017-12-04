from py4j.java_gateway import JavaGateway, java_import, get_field, CallbackServerParameters, GatewayParameters
from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import uuid
import time
import logging

logger = logging.getLogger("py4j.java_gateway")
logger.setLevel(10)
consoleHandler = logging.StreamHandler()
logger.addHandler(consoleHandler)

# ------------------------------------------------------
_async_registry = {}
    
gateway = ClientServer(
    java_parameters=JavaParameters(auto_field=True), 
    python_parameters=PythonParameters(daemonize=True))

def shutdown_when_done():
    while _async_registry:
        print(_async_registry)
        time.sleep(1)
    print('Shutting down connection')
    gateway.close()
    print('Done')
    
class AsyncTask(object):
    def __init__(self):
        self.id = uuid.uuid4()
        _async_registry[self.id] = self
    def remove(self):
        del _async_registry[self.id]

class PythonCallbackImpl(AsyncTask):
    def __init__(self, execfunc):
        super().__init__()
        self.execfunc = execfunc
    def callback(self):
        print('[PythonCallbackImpl] notified from Java')
        self.execfunc()
        self.remove()
    class Java:
        implements = ["com.macuyiko.minecraftpyserver.javabridge.PythonCallback"]

# ------------------------------------------------------

java_import(gateway.jvm, 'org.bukkit.*')
java_import(gateway.jvm, 'com.macuyiko.minecraftpyserver.javabridge.*')

BUKKIT = gateway.jvm.Bukkit
SERVER = BUKKIT.getServer()
WORLD = SERVER.getWorlds().get(0)
PLUGIN = SERVER.getPluginManager().getPlugin('MinecraftPyServer')

p = SERVER.getPlayer("Macuyiko")
print(p.getLocation())


def run_synchronous(execfunc, delay=None):
    python_callback = PythonCallbackImpl(execfunc)
    spigot_runnable = gateway.jvm.SpigotRunnable(python_callback)
    if delay is None: spigot_runnable.runTask(PLUGIN)
    else: spigot_runnable.runTaskLater(PLUGIN, delay)

def fun():
    print('calling fun')
    gateway.jvm.System.out.println("Hello from python!")
    p = SERVER.getPlayer("Macuyiko")
    l = p.getLocation()
    WORLD.createExplosion(l.getX(), l.getY(), l.getZ(), 3.0, True)

run_synchronous(fun, None)

time.sleep(5)
shutdown_when_done()