from py4j.java_gateway import JavaGateway, java_import, get_field, CallbackServerParameters, GatewayParameters
from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import threading
import sys

main_thread = threading.current_thread()

class PythonCallbackImpl(object):
    def __init__(self, execfunc):
        self.execfunc = execfunc
    def callback(self):
        print('[PythonCallbackImpl] notified from Java')
        self.execfunc()
        sys.exit()
    class Java:
        implements = ["javabridge.test.PythonCallback"]

gateway = ClientServer(
    java_parameters=JavaParameters(), 
    python_parameters=PythonParameters(daemonize=True))

java_import(gateway.jvm, 'javabridge.test.*')


def simple_fun():
    print('[simple_fun] called', threading.current_thread())
    gateway.jvm.System.out.println("[simple_fun] Hello from python!")
    currentThread = gateway.jvm.Thread.currentThread();
    print('[simple_fun] thread is', currentThread.getId())

python_callback = PythonCallbackImpl(simple_fun)
executor = gateway.jvm.Test(python_callback)
executor.runAsynchronous()

input('Press ENTER to close')

for t in threading.enumerate():
    if t is main_thread:
        continue
    print('t ', t, t.getName())
