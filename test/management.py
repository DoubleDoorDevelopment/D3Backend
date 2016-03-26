
from subprocess import Popen, PIPE
from threading import Thread

def watchStream(manager, stream):
	for line in stream:
		manager.outputFunc(line)
	if not stream.closed:
		stream.close()

class Manager():
	
	def __init__(self, name, processArgs, funcToAppendLine):
		self.process = Popen(processArgs, stdout=PIPE, stderr=PIPE)
		self.outputFunc = funcToAppendLine
		Thread(
			target = watchStream, name = name + ' stdout-watcher',
			args = (self, self.process.stdout)
		).start()
		Thread(
			target = watchStream, name = name + ' stderr-watcher',
			args = (self, self.process.stderr)
		).start()
	
	def isRunning(self):
		return self.process.poll() is not None

def outputLine(line):
	print line

manager = Manager("Owner_ServerName", ["ls"], outputLine)

