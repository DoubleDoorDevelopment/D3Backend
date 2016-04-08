
import Base
import os
import json
import urllib
import requests
import subprocess
from werkzeug import secure_filename
from configparser import ConfigParser
from socket import error as SocketError

class Data(Base.Data):
	
	def __init__(self):
		Base.Data.__init__(self)
		self.setEdittableFiles({})
	
	def getPortOriginal(self):
		return 34197
	
	def getPortRange(self):
		return (34100, 34199)
	
	def getRunConfigKeys(self):
		return ['version']

class Cache(Base.Cache):
	
	def __init__(self, directory):
		Base.Cache.__init__(self, directory)
		self.url_manifest = 'https://www.factorio.com/updater/get-available-versions'
		self.url_version = 'https://www.factorio.com/updater/get-download-link'
		self.url_version_new = 'https://www.factorio.com/get-download/%VERSION%/headless/linux64'
		self.packages = None
		self.package = 'core-linux_headless64'
		self.stable = None
	
	def getNames(self):
		return ['Factorio']
	
	def refreshCache(self, typeCache, force = False):
		if force or not os.path.exists(self.getManifestPath()):
			self.addDownloadFunc(self.downloadManifest)
		else:
			self.loadManifest()
	
	def getManifestPath(self):
		return self.dirCache + "manifest.json"
	
	def downloadManifest(self):
		data = json.loads(urllib.urlopen(self.url_manifest).read())
		
		# packages
		# -> package (i.e. core-linux_headless64)
		#    -> stable
		#       -> 0.12.29
		#       -> ...
		#    -> versions
		#       -> 0.12.7
		#          -> link
		#       -> ...
		self.packages = {}
		
		oldData = self.readManifest()
		for package in oldData:
			self.packages[package] = { 'stable': oldData[package]['stable'] }
		
		for package in data:
			versions = []
			stable = []
			
			for versionRow in data[package]:
				if 'from' in versionRow:
					# Get all the from versions
					version = versionRow['from']
					if not version is None and not version in versions:
						print("Found Factorio " + package + " " + version)
						versions.append(version)
					# Get all the to versions
					version = versionRow['to']
					if not version is None and not version in versions:
						print("Found Factorio " + package + " " + version)
						versions.append(version)
				elif 'stable' in versionRow:
					stable.append(versionRow['stable'])
			
			versions = sorted(versions, key=lambda version: [int(x) for x in version.split('.')])
			
			if not package in self.packages:
				self.packages[package] = {}
			
			if not 'versions' in self.packages[package]:
				self.packages[package]['versions'] = []
			if not 'stable' in self.packages[package]:
				self.packages[package]['stable'] = []
			
			self.packages[package]['versions'].extend(versions)
			self.packages[package]['stable'].extend(stable)
		
		Base.dumpJson(self.packages, self.getManifestPath())
	
	def loadManifest(self):
		self.packages = self.readManifest()
	
	def readManifest(self):
		if os.path.exists(self.getManifestPath()):
			with open(self.getManifestPath(), 'r') as file:
				return json.load(file)
		else:
			return {}
	
	def getVersions(self, typeVersion = None, data = {}):
		return self.packages[self.package]['versions']
	
	def getVersionsStable(self):
		return self.packages[self.package]['stable']
	
	def getVersionURL(self, typeVersion, data = {}):
		verFrom = data['from']
		verTo = data['to']
		if verFrom is None:
			return self.url_version_new.replace("%VERSION%", verTo), None
		else:
			getData = {
				'package': self.package,
				'apiVersion': 2,
				'from': verFrom,
				'to': verTo
			}
			req = requests.get(self.url_version, params = getData)
			if req.status_code == 200:
				return req.json()[0], None
			else:
				return None, req.status_code
		return None, None

class Server(Base.Server):
	
	def __init__(self, cache, directory, nameOwner, nameServer):
		Base.Server.__init__(self, cache, directory, nameOwner, nameServer)
	
	def setPort(self, port):
		filePath = self.dirRun + "factorio/config/config.ini"
		config = ConfigParser()
		config.read(filePath)
		config['other']['port'] = str(port)
		with open(filePath, 'w') as file:
			config.write(file)
	
	def backup(self):
		pass
	
	def cleanRunFiles(self):
		path = self.dirRun
		exts = ['tar.gz', 'zip']
		for filename in os.listdir(path):
			filePath = os.path.join(path, filename)
			if os.path.isfile(filePath):
				if filename.split('.')[-1] in exts:
					os.remove(filePath)
	
	def install(self, **kwargs):
		func = kwargs['func']
		data = kwargs['data']
		files = kwargs['files']
		if func == 'server':
			self.backup()
			self.cleanRunFiles()
			
			if 'save' in files:
				file = files['save']
				if file and '.' in file.filename and file.filename.rsplit('.', 1)[1] in ['zip']:
					filename = secure_filename(file.filename)
					
					saves = self.dirRun + "factorio/saves/"
					if not os.path.exists(saves):
						os.mkdir(saves)
					
					filePath = saves + filename
					file.save(filePath)
					
					if not 'saves' in data:
						data['saves'] = []
					data['saves'].append(filename)
					data['save'] = filename
			
			updateData = {}
			if 'saves' in data:
				updateData['saves'] = data['saves']
			if 'save' in data:
				updateData['save'] = data['save']
			self.updateRunConfig(updateData)
			
			version = data['version']
			runConfig = self.getRunConfig()
			if 'version' in runConfig:
				currentVer = runConfig['version']
			else:
				currentVer = None
			print("Factorio: Dwnld: " + str(currentVer) + " -> " + str(version))
			self.addDownloadFunc(self.downloadAndInstall, verFrom = currentVer, verTo = version, data = data)
	
	def downloadAndInstall(self, **kwargs):
		verFrom = kwargs['verFrom']
		verTo = kwargs['verTo']
		data = kwargs['data']
		url, error = self.cache.getVersionURL(None, data = { 'from': verFrom, 'to': verTo })
		isFirstInstall = verFrom is None
		if url != None:
			env = dict(os.environ)
			FNULL = open(os.devnull, 'w')
			command = None
			if verFrom is None: # first install
				filename = verTo + ".tar.gz"
				filePath = self.dirRun + filename
				command = ["tar", "-xzf", filename]
			else:
				filename = url.split('?')[0].split('/')[-1]
				filePath = self.dirRun + filename
				command = ['./factorio/bin/x64/factorio', '--apply-update', filePath]
			print("Fetching Factorio server file at: " + url)
			try:
				#urllib.urlretrieve(url, filePath)
				Base.downloadFile(url, filePath)
				subprocess.call(command, env=env, cwd=self.dirRun, stdout=FNULL, stderr=subprocess.STDOUT)
			except SocketError as e:
				print(url)
				print(filePath)
				print(str(e))
			except Exception as e:
				print(str(e))
			self.updateRunConfig(data)

class Thread(Base.Thread):
	
	def __init__(self, server):
		Base.Thread.__init__(self, server)
	
	def setRunArgs(self):
		save = self.getSaveName()
		if save is None:
			self.runArgs = None
		else:
			self.runArgs = [
				'./factorio/bin/x64/factorio',
				'--start-server',
				save
			]
	
	def getSaveName(self):
		runConfig = self.server.getRunConfig()
		if 'save' in runConfig:
			return runConfig['save']
		else:
			return None
