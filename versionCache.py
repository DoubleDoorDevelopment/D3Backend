
import urllib, json, os.path
from customthreading import *

downloaders = None

class UrlFile():
	
	def __init__(self, path, name, extension, url):
		self.path = path
		self.name = name
		self.extension = extension
		self.url = url
	
	def getFilePath(self):
		return self.path + self.name + "." + self.extension
	
	def doesExist(self):
		return os.path.isfile(getFilePath(self))
	
	def download(self):
		global downloaders
		if not doesExist(self):
			downloaders.addDownload(self.url, getFilePath(self))

class Cache():
	
	def __init__(self, path):
		self.dir_path = path
	
	def refresh(self):
		pass

class CacheMinecraft(Cache):
	
	def __init__(self, path):
		Cache.__init__(self, path)
		self.url_manifest_vanilla = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
		self.dir_vanilla = "vanilla/"
		self.versions_vanilla = None
		self.url_manifest_forge = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json"
		self.dir_forge = "forge/"
		self.url_file_forge = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/%ID%/forge-%ID%-universal.jar"
		self.versions_forge = None
	
	def getManifestPathVanilla(self):
		return self.dir_path + self.dir_vanilla + "manifest.json"
	
	def getManifestPathForge(self):
		return self.dir_path + self.dir_forge + "manifest.json"
	
	def refresh(self):
		self.refreshManifest()
		self.refreshDownloads()
	
	def refreshManifest(self):
		self.refreshManifestVanilla()
		self.refreshManifestForge()
	
	def refreshManifestVanilla(self):
		if not os.path.exists(self.getManifestPathVanilla()):
			downloaders.addDownloadFunc(self.downloadManifestVanilla)
	
	def downloadManifestVanilla(self):
		data = json.loads(urllib.urlopen(self.url_manifest_vanilla).read())
		# ReleaseType -> Version -> { jsonURL, serverURL }
		self.versions_vanilla = {}
		# For every entry in the version list
		for entry in data['versions']:
			# get the type of the entry
			versionType = entry['type']
			print("Downloading data for vanilla version " + entry['id'])
			
			# add entry to manifest
			urls = self.getManifestEntryVanilla(entry['id'], entry['url'])
			if urls != None:
				
				# check the versions for the version type
				if not versionType in self.versions_vanilla:
					self.versions_vanilla[versionType] = {}
				
				self.versions_vanilla[versionType][entry['id']] = urls
		
		self.dumpJson(self.versions_vanilla, self.getManifestPathVanilla())
	
	def getManifestEntryVanilla(self, version, versionJsonUrl):
		data = json.loads(urllib.urlopen(versionJsonUrl).read())
		downloads = data['downloads']
		if 'server' in downloads:
			if 'url' in downloads['server']:
				url_server = downloads['server']['url']
				jarPath = self.dir_path + self.dir_vanilla + version + ".jar"
				if not os.path.exists(jarPath):
					downloaders.addDownload(url_server, jarPath)
				return { 'url_json': versionJsonUrl, 'url_server': url_server }
		return None
	
	def refreshManifestForge(self):
		if not os.path.exists(self.getManifestPathForge()):
			downloaders.addDownloadFunc(self.downloadManifestForge)
	
	def downloadManifestForge(self):
		data = json.loads(urllib.urlopen(self.url_manifest_forge).read())
		self.versions_forge = {}
		for build in data['number'].itervalues():
			branch = build['branch']
			buildNumber = build['build']
			version_minecraft = build['mcversion']
			version_forge = build['version']
			buildID = version_minecraft + "-" + version_forge
			if branch != None:
				buildID += "-" + branch
			
			url_server = self.url_file_forge.replace("%ID%", buildID)
			
			if not version_minecraft in self.versions_forge:
				self.versions_forge[version_minecraft] = {}
			self.versions_forge[version_minecraft][buildID] = url_server
			
			jarPath = self.dir_path + self.dir_forge + "forge-" + buildID + ".jar"
			if not os.path.exists(jarPath):
				downloaders.addDownload(url_server, jarPath)
		
		self.dumpJson(self.versions_forge, self.getManifestPathForge())
	
	def refreshDownloads(self):
		pass
	
	def dumpJson(self, data, file):
		with open(file, 'w') as outfile:
			json.dump(data, outfile, sort_keys=True, indent=4, separators=(',', ': '))

cache_versions = {}

def init(gameTypes, path_run):
	global downloaders
	downloaders = ThreadMaster(ThreadPool(4))
	
	path_cache = path_run + "cache/"
	path_versions = path_cache + "versions/"
	
	for game in gameTypes:
		cache_versions[game] = gameTypes[game]['cache'](path_versions + game + "/")
	
	for game in cache_versions:
		cache_versions[game].refresh()
	