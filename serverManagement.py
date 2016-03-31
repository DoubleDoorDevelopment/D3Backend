
from threading import Thread
from subprocess import Popen, PIPE, STDOUT
import os

import io
import sys

def getJava():
	#return [["0", "java"]]
	jvm = '/usr/lib/jvm'
	
	javaVersions = []
	for file in os.listdir(jvm):
		path = os.path.join(jvm, file)
		if (not os.path.islink(path) and
			os.path.isdir(path) and
			os.path.isdir(os.path.join(path, "bin"))
		):
			fileParts = file.split('-')
			javaVersions.append([fileParts[1], path])
	return sorted(javaVersions, reverse=True)

class Server(Thread):

	def __init__(self, nameOwner, nameServer, directory, runConfig):
		Thread.__init__(self)
		
		self.directory = directory
		self.nameOwner = nameOwner
		self.nameServer = nameServer
		self.name = "ThreadServer:" + self.nameOwner + ":" + self.nameServer

		self.output = ''

		self.runConfig = runConfig
		self.setRunArgs()

	def setRunArgs(self):
		self.javaVersions = getJava()
		self.runArgs = [
			self.javaVersions[0][1] + "/bin/java",
			'-server',
			'-Xms{0}M'.format(self.runConfig['ram_min']),
			'-Xmx{0}M'.format(self.runConfig['ram_max']),
			'-XX:MaxPermSize={0}m'.format(self.runConfig['perm_gen'])
		]
		params = self.runConfig['other_java_params']
		if not params == '':
			for param in params.split(' '):
				self.runArgs.append(param)
		self.runArgs.extend([
			'-jar', self.getJarName(),
			'nogui'
		])
		params = self.runConfig['other_mc_params']
		if not params == '':
			for param in params.split(' '):
				self.runArgs.append(param)

	def getRunArgs(self):
		return self.runArgs

	def run(self):
		path = self.getLogPath()
		writer = io.open(path, 'wb')
		
		writer.write("----=====##### SERVER PROCESS  STARTING #####=====-----\n")
		writer.flush()
		
		self.process = Popen(self.getRunArgs(), cwd = self.directory, stdout = PIPE, stderr = STDOUT, stdin = PIPE)
		while self.process.poll() is None:
			out = self.process.stdout.read(1)
			if out != '':
				writer.write(out)
				writer.flush()
				#sys.stdout.write(out.decode("utf-8"))
				#sys.stdout.flush()
		
		writer.write("----=====##### SERVER PROCESS HAS ENDED #####=====-----\n")
		writer.flush()
		writer.close()

	def stop(self):
		#self.process.stdin.write('/stop')
		self.process.terminate()
	
	def kill(self):
		self.process.kill()

	def send(self, cmd):
		cmdFormatted = "/{0}\r\n".format(cmd)
		cmdBytes = cmdFormatted.encode("ascii")
		self.process.stdin.write(cmdBytes)
		self.process.stdin.flush()
	
	def getJarName(self):
		return "minecraft_server.1.9.jar"
	
	def getLogFileName(self):
		return "console.log"
	
	def getLogPath(self):
		return os.path.join(self.directory, self.getLogFileName())
