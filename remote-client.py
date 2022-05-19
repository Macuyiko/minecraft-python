from code import InteractiveConsole
import sys
try:
    import websocket 
except:
    print('Please install websocket_client')
    raise

class InteractiveRemoteConsole(InteractiveConsole):
    def __init__(self, uri=None):
        if uri is None:
            uri = 'ws://localhost:44445/'
        InteractiveConsole.__init__(self, None, "<remoteconsole>")
        self.websocket = websocket.create_connection(uri)
        self.websocket.settimeout(60)

    def interact(self, banner=None, exitmsg=None):
        if banner is None:
            self.write("Remote Python to Minecraft\n")
        elif banner:
            self.write("%s\n" % str(banner))
        self.recv()
        while 1:
            try:
                try:
                    line = self.raw_input()
                except EOFError:
                    self.write("\n")
                    break
                else:
                    self.push(line)
                    self.recv()
            except KeyboardInterrupt:
                self.write("\nKeyboardInterrupt\n")
                break
        self.websocket.close()
        if exitmsg is None:
            self.write('now exiting %s...\n' % self.__class__.__name__)
        elif exitmsg != '':
            self.write('%s\n' % exitmsg)
    
    def recv(self, supress_prompt=False):
        result = None
        while result is None or (not result.endswith('>>> ') and not result.endswith('... ')):
            result = self.websocket.recv()
            if not supress_prompt or (not result.endswith('>>> ') and not result.endswith('... ')):
                print(result, end = '')
    
    def push(self, line):
        self.websocket.send(line)

def interact(uri=None, readfunc=None, banner=None, exitmsg=None):
    console = InteractiveRemoteConsole(uri)
    if readfunc is not None:
        console.raw_input = readfunc
    else:
        try:
            import readline
        except ImportError:
            pass
    console.interact(banner, exitmsg)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        uri = None if len(sys.argv) <= 2 else sys.argv[2]
        source = sys.argv[1]
        console = InteractiveRemoteConsole(uri)
        with open(source, 'r') as sourcefile:
            # Wait for initial prompt
            console.recv(supress_prompt=True)
            # Send lines
            for line in sourcefile:
                line = line.rstrip()
                console.push(line)
                console.recv(supress_prompt=True)
            # Add final new lines
            console.push("")
            console.recv(supress_prompt=True)
            console.push("")
            console.recv(supress_prompt=True)
    else:
        interact()
