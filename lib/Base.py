
from types import TypeCache
import types as Types

import json

import threading
from subprocess import Popen, PIPE, STDOUT
import os
import io
import sys
import urllib2
import ssl

from customthreading import ThreadMaster, ThreadPool

downloaders = None

def initDownloaders():
	global downloaders
	downloaders = ThreadMaster(ThreadPool(4))

def dumpJson(data, file):
	with open(file, 'w') as outfile:
		json.dump(data, outfile, sort_keys=True, indent=4, separators=(',', ': '))

def downloadFile(url, filePath):
	try:
		request = urllib2.Request(url, headers={ 'X-Mashape-Key': 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX' })
		gcontext = ssl.SSLContext(ssl.PROTOCOL_TLSv1)
		urlFile = urllib2.urlopen(request, context = gcontext)
		print("Downloading " + url)
		
		with open(filePath, 'wb') as file:
			file.write(urlFile.read())
		
	except urllib2.HTTPError, e:
		print("HTTP Error: ", e.code, url)
	except urllib2.URLError, e:
		print("URL Error: ", e.reason, url)
	except Exception, e:
		print("Error: ", str(e))

class Data:
	
	def __init__(self):
		self.edittableFiles = {}
	
	def getPortOriginal(self):
		return 0
	
	def getPortRange(self):
		return (0, 0)
	
	def setEdittableFiles(self, dictionary):
		self.edittableFiles = dictionary
	
	def getEdittableFileKeys(self):
		return self.edittableFiles.keys()
	
	def getEdittableFile(self, key):
		return self.edittableFiles[key]
	
	def getRunConfigKeys(self):
		return []

class Downloader:
	
	def addDownloadFunc(self, func, *args, **kargs):
		downloaders.addDownloadFunc(func, *args, **kargs)
	
	def addDownload(self, url, filePath):
		downloaders.addDownload(url, filePath)

class Cache(Downloader):
	
	def __init__(self, directory):
		self.dirCache = directory
	
	def refresh(self, force = False):
		self.refreshCache(TypeCache.ALL, force)
	
	def refreshCache(self, typeCache, force = False):
		pass
	
	def getVersions(self, typeVersion, data = {}):
		return []
	
	def getVersionURL(self, typeVersion, data = {}):
		return None

class Server(Downloader):
	
	def __init__(self, cache, directoryPath, nameOwner, nameServer):
		self.cache = cache
		self.dirRun = directoryPath
		self.nameOwner = nameOwner
		self.nameServer = nameServer
		self.typeServer = None
		self.thread = None
		self.downloading = False
		self.errors = {}
	
	def setTypeServer(self, typeServer):
		self.typeServer = typeServer
	
	def getTypeServer(self):
		return self.typeServer
	
	def setCurrentThread(self, thread):
		self.thread = thread
	
	def start(self):
		if not self.isOnline():
			typeServer = Types.serverTypes[self.getTypeServer()]
			typeServer.getClassThread()(self).start()
	
	def stop(self):
		if self.isOnline():
			self.thread.stop()
	
	def kill(self):
		if self.isOnline():
			self.thread.kill()
	
	def send(self, cmd):
		if self.isOnline():
			self.thread.send(cmd)
	
	def setPort(self, port):
		pass
	
	def install(self, **kwargs):
		self.backup()
	
	def backup(self):
		pass
	
	def cleanRunFiles(self):
		pass
	
	def isOnline(self):
		return not self.thread is None
	
	def setDownloading(self, isDownloading):
		self.downloading = isDownloading
	
	def isDownloading(self):
		return self.downloading
	
	def setErrors(self, key, data):
		self.errors[key] = data
	
	def getErrors(self):
		return self.errors
	
	def removeError(self, category, index):
		del self.errors[category][index]
		if len(self.errors[category]) <= 0:
			self.errors.pop(category, None)
	
	def getRunConfig(self):
		filePath = self.dirRun + "run.json"
		if os.path.exists(filePath):
			with open(filePath, 'r') as file:
				return json.load(file)
		return {}
	
	def updateRunConfig(self, newData):
		filePath = self.dirRun + "run.json"
		
		oldData = self.getRunConfig()
		
		for key in newData:
			oldData[key] = newData[key]
		
		dumpJson(oldData, filePath)

class Thread(threading.Thread):
	
	def __init__(self, server):
		threading.Thread.__init__(self)
		self.name = "ThreadServer:" + server.nameOwner + ":" + server.nameServer
		self.server = server
	
	def getLogFileName(self):
		return "console.log"
	
	def getLogPath(self):
		return os.path.join(self.server.dirRun, self.getLogFileName())
	
	def setRunArgs(self):
		self.runArgs = []
	
	def getRunArgs(self):
		return self.runArgs
	
	def start(self):
		self.server.setCurrentThread(self)
		self.setRunArgs()
		threading.Thread.start(self)
	
	def run(self):
		pathLog = self.getLogPath()
		writer = io.open(pathLog, 'wb')
		
		writer.write("----=====##### SERVER PROCESS  STARTING #####=====-----\n")
		writer.flush()
		
		runArgs = self.getRunArgs()
		if runArgs is None:
			writer.write("Invalid runtime parameters, please ask server admins WHYYYYY?!?!?!\n")
			writer.flush()
		else:
			self.process = Popen(runArgs, cwd = self.server.dirRun,
					stdout = PIPE, stderr = STDOUT, stdin = PIPE)
			while self.process.poll() is None:
				out = self.process.stdout.read(1)
				if out != '':
					writer.write(out)
					writer.flush()
		
		writer.write("----=====##### SERVER PROCESS HAS ENDED #####=====-----\n")
		writer.flush()
		writer.close()
		self.server.setCurrentThread(None)
	
	def stop(self):
		self.process.terminate()
		self.server.setCurrentThread(None)
	
	def kill(self):
		self.process.kill()
		self.server.setCurrentThread(None)
	
	def send(self, cmd):
		cmdFormatted = self.formatCommand(cmd)
		cmdBytes = cmdFormatted.encode("ascii")
		self.process.stdin.write(cmdBytes)
		self.process.stdin.flush()
	
	def formatCommand(self, cmd):
		return cmd
