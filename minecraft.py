
from threading import Thread
import urllib
import os
import shutil
import zipfile

def installModpack(nameOwner, nameServer, filePath, fileName, isCurse = False):
	
	import backend
	allServers = backend.getConfig('SERVERS_DIRECTORY')
	dirName = nameOwner + "_" + nameServer + "/"
	serverDir = allServers + dirName
	modpackDir = serverDir + "modpack/"
	destFilePath = modpackDir + "modpack.zip"
	
	os.mkdir(modpackDir)
	shutil.move(filePath, destFilePath)
	
	with zipfile.ZipFile(destFilePath, 'r') as z:
		z.extractall(modpackDir)
	
	if isCurse:
		# Zip file structure:
		# file
		#  -> modlist.html
		#  -> manifest.json
		#  -> overrides/
		#      -> mods
		#      -> config
		
		pass
	else:
		# Zip file structure:
		# file
		#  -> mods
		#  -> config
		#  -> ...
		shutil.move(modpackDir + "config/", serverDir + "config/")
		shutil.move(modpackDir + "mods/", serverDir + "mods/")
	
	shutil.rmtree(modpackDir)

class ThreadDownloadModpack(Thread):
	
	def __init__(self, nameOwner, nameServer, url, filePath, fileName):
		Thread.__init__(self)
		self.url = url
		self.nameOwner = nameOwner
		self.nameServer = nameServer
		self.filePath = filePath
		self.fileName = fileName
	
	def run(self):
		urllib.urlretrieve(self.url, self.filePath)
		installModpack(self.nameOwner, self.nameServer, self.filePath, self.fileName)

class ThreadInstallModpack(Thread):
	
	def __init__(self, nameOwner, nameServer, filePath, fileName):
		Thread.__init__(self)
		self.nameOwner = nameOwner
		self.nameServer = nameServer
		self.filePath = filePath
		self.fileName = fileName
	
	def run(self):
		installModpack(self.nameOwner, self.nameServer, self.filePath, self.fileName)