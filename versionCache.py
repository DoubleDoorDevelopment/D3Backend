
import urllib, json
import os, os.path
from customthreading import *
import json

downloaders = None

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
		# ReleaseType -> Version -> { jsonURL, serverURL }
		self.versions_vanilla = {}
		self.url_manifest_forge = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json"
		self.dir_forge = "forge/"
		self.url_file_forge = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/%ID%/forge-%ID%-universal.jar"
		self.versions_forge = {}
		self.loadManifest()
	
	def getManifestPathVanilla(self):
		return self.dir_path + self.dir_vanilla + "manifest.json"
	
	def getManifestPathForge(self):
		return self.dir_path + self.dir_forge + "manifest.json"
	
	def loadManifest(self):
		self.loadManifestVanilla()
		self.loadManifestForge()
	
	def loadManifestVanilla(self):
		data = self.getJson(self.getManifestPathVanilla())
		if data != None:
			self.versions_vanilla = data
	
	def loadManifestForge(self):
		data = self.getJson(self.getManifestPathForge())
		if data != None:
			self.versions_forge = data
	
	def getJson(self, filePath):
		if os.path.exists(filePath):
			with open(filePath) as data_file:
				return json.load(data_file)
		return None
	
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
		self.versions_vanilla = {}
		# For every entry in the version list
		for entry in data['versions']:
			# get the type of the entry
			versionType = entry['type']
			
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
				return { 'url_json': versionJsonUrl, 'url_server': url_server }
		return None
	
	def refreshManifestForge(self):
		if not os.path.exists(self.getManifestPathForge()):
			downloaders.addDownloadFunc(self.downloadManifestForge)
	
	def downloadManifestForge(self):
		data = json.loads(urllib.urlopen(self.url_manifest_forge).read())
		self.version_forge = {}
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
		
		self.dumpJson(self.versions_forge, self.getManifestPathForge())
	
	def refreshDownloads(self, force=False):
		self.refreshDownloadsVanilla(force=force)
		self.refreshDownloadsForge(force=force)
	
	def refreshDownloadsVanilla(self, force):
		for versionType in self.versions_vanilla:
			for version in self.versions_vanilla[versionType]:
				jarPath = self.dir_path + self.dir_vanilla + version + ".jar"
				if force and os.path.exists(jarPath):
					os.remove(jarPath)
				if not os.path.exists(jarPath):
					if 'url_server' in self.versions_vanilla[versionType]:
						url_server = self.versions_vanilla[versionType]['url_server']
						downloaders.addDownload(url_server, jarPath)
	
	def refreshDownloadsForge(self, force):
		for version_minecraft in self.versions_forge:
			for buildID in self.versions_forge[version_minecraft]:
				url_server = self.versions_forge[version_minecraft][buildID]
				jarPath = self.dir_path + self.dir_forge + buildID + ".jar"
				if force and os.path.exists(jarPath):
					os.remove(jarPath)
				if not os.path.exists(jarPath):
					downloaders.addDownload(url_server, jarPath)
	
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
	