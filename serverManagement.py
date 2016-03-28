
from threading import Thread
import io
import subprocess
import shutil
import time
import os

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
		self.runConfig = runConfig
		self.name = "ThreadServer:" + self.nameOwner + ":" + self.nameServer
		self.setRunArgs()
	
	def getLogFileName(self):
		return "console.log"
	
	def getLogPath(self):
		return os.path.join(self.directory, self.getLogFileName())
	
	def setRunArgs(self):
		self.javaVersions = getJava()
		self.runArgs = [
			self.javaVersions[0][1] + "/bin/java",
			'-server',
			'-Xms{0}M'.format(self.runConfig['ram_min']),
			'-Xmx{0}M'.format(self.runConfig['ram_max']),
			'-XX:MaxPermSize={0}m'.format(self.runConfig['perm_gen'])
		]
		for param in self.runConfig['other_java_params'].split(' '):
			self.runArgs.append(param)
		self.runArgs.extend([
			'-jar', self.getJarName(),
			'nogui'
		])
		for param in self.runConfig['other_mc_params'].split(' '):
			self.runArgs.append(param)

	def getRunArgs(self):
		return self.runArgs
	
	def run(self):
		path = self.getLogPath()
		with io.open(path, 'wb') as writer:
			try:
				self.process = subprocess.Popen(self.getRunArgs(), cwd=self.directory, stdout = writer)
			except Exception as e:
				print(str(e))
				return
		while self.process.poll() is None:
			continue
	
	def stop(self):
		#self.process.stdin.write('/stop')
		self.process.terminate()
	
	def kill(self):
		self.process.kill()
	
	def getJarName(self):
		return "minecraft_server.1.9.jar"
