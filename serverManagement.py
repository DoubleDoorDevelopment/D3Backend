
from threading import Thread
import io
import subprocess
import os
import shutil
import time

class Server(Thread):
	
	def __init__(self, nameOwner, nameServer, directory):
		Thread.__init__(self)
		self.directory = directory
		self.nameOwner = nameOwner
		self.nameServer = nameServer
		self.setRunArgs()
	
	def getLogFileName(self):
		return "console.log"
	
	def getLogPath(self):
		return self.directory + self.getLogFileName()
	
	def loadLogFile(self):
		path = self.getLogPath()
		return io.open(path, 'wb')
	
	def setRunArgs(self):
		self.runArgs = [
			'java', '-server',
			'-jar', self.getJarName(),
			'nogui'
		]
	
	def getRunArgs(self):
		return self.runArgs
	
	def run(self):
		writer = self.loadLogFile()
		self.process = subprocess.Popen(self.getRunArgs(), stdout = writer)
		while self.process.poll() is None:
			continue
	
	def stop(self):
		pass
	
	def kill(self):
		pass
	
	def getJarName(self):
		return "minecraft_server.1.9.jar"
