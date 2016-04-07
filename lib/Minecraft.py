
import Base
from types import TypeCache
import os
import json
import urllib
import shutil
import urllib2
import threading
import zipfile
import subprocess

class Data(Base.Data):
	
	def __init__(self):
		Base.Data.__init__(self)
		self.setEdittableFiles({
			'BannedIPS':		'banned-ips.json',
			'BannedPlayers':	'banned-players.json',
			'Ops':				'ops.json',
			'Properties':		'server.properties',
			'Whitelist':		'whitelist.json'
		})
	
	def getPortOriginal(self):
		return 25500
	
	def getPortRange(self):
		return (25500, 25599)
	
	def getRunConfigKeys(self):
		return [
			'ip',
			'other_java_params',
			'other_mc_params',
			'perm_gen',
			'ram_max',
			'ram_min',
			'version_forge',
			'version_minecraft'
		]

class Cache(Base.Cache):
	
	def __init__(self, directory):
		Base.Cache.__init__(self, directory)
		
		self.url_manifest_vanilla = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
		self.dir_vanilla = "vanilla/"
		# ReleaseType -> Version -> { jsonURL, serverURL }
		self.versions_vanilla = {}
		
		self.url_manifest_forge = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json"
		self.dir_forge = "forge/"
		self.url_file_forge = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/%ID%/forge-%ID%-installer.jar"
		self.versions_forge = {}
	
	def refreshCache(self, typeCache, force = False):
		self.refreshManifestVanilla(force)
		self.refreshManifestForge(force)
	
	def getManifestPathVanilla(self):
		return self.dirCache + self.dir_vanilla + "manifest.json"
	
	def refreshManifestVanilla(self, force):
		if force or not os.path.exists(self.getManifestPathVanilla()):
			self.addDownloadFunc(self.downloadManifestVanilla)
		else:
			self.loadManifestVanilla()
	
	def downloadManifestVanilla(self):
		data = json.loads(urllib.urlopen(self.url_manifest_vanilla).read())
		self.versions_vanilla = {}
		# For every entry in the version list
		for entry in data['versions']:
			# get the type of the entry
			versionType = entry['type']
			print("Found Vanilla " + versionType + " " + entry['id'])
			# add entry to manifest
			urls = self.getManifestEntryVanilla(entry['id'], entry['url'])
			if urls != None:
				urls['type'] = versionType
				self.versions_vanilla[entry['id']] = urls
		Base.dumpJson(self.versions_vanilla, self.getManifestPathVanilla())
	
	def getManifestEntryVanilla(self, version, versionJsonUrl):
		data = json.loads(urllib.urlopen(versionJsonUrl).read())
		downloads = data['downloads']
		if 'server' in downloads:
			if 'url' in downloads['server']:
				url_server = downloads['server']['url']
				return { 'url_json': versionJsonUrl, 'url_server': url_server }
		return None
	
	def loadManifestVanilla(self):
		with open(self.getManifestPathVanilla(), 'r') as file:
			data = json.load(file)
			self.versions_vanilla = data
	
	def getVersionsVanillaSorted(self):
		return sorted(self.versions_vanilla)
	
	def getManifestPathForge(self):
		return self.dirCache + self.dir_forge + "manifest.json"
	
	def refreshManifestForge(self, force):
		if force or not os.path.exists(self.getManifestPathForge()):
			self.addDownloadFunc(self.downloadManifestForge)
		else:
			self.loadManifestForge()
	
	def downloadManifestForge(self):
		data = json.loads(urllib.urlopen(self.url_manifest_forge).read())
		self.versions_forge = {}
		for build in data['number'].itervalues():
			print(build)
			branch = build['branch']
			buildNumber = build['build']
			version_minecraft = build['mcversion']
			version_forge = build['version']
			
			buildID = ""
			if not version_minecraft is None:
				buildID += version_minecraft + "-"
			buildID += version_forge
			if not branch is None:
				buildID += "-" + branch
			
			print("Found forge " + buildID)
			url_server = self.url_file_forge.replace("%ID%", buildID)
			
			if not version_minecraft in self.versions_forge:
				self.versions_forge[version_minecraft] = {}
			self.versions_forge[version_minecraft][buildID] = url_server
		
		Base.dumpJson(self.versions_forge, self.getManifestPathForge())
	
	def loadManifestForge(self):
		with open(self.getManifestPathForge(), 'r') as file:
			self.versions_forge = json.load(file)
	
	def getVersionsForgeSorted(self):
		return self.versions_forge
	
	def getVersions(self, typeVersion, data = {}):
		if 'which' in data:
			which = data['which']
			if which == "minecraft":
				pass
			elif which == "forge":
				pass
		else:
			return []
	
	def getVersionURL(self, typeVersion, data = {}):
		version = data['version']
		if typeVersion == 'vanilla':
			return self.versions_vanilla[version]['url_server']
		elif typeVersion == 'forge':
			return self.versions_forge[data['mc']][version]

class Server(Base.Server):
	
	def __init__(self, cache, directoryPath, nameOwner, nameServer):
		Base.Server.__init__(self, cache, directoryPath, nameOwner, nameServer)
	
	def setPort(self, port):
		pass
	
	def backup(self):
		pass
	
	def cleanRunFiles(self):
		path = self.dirRun
		exts = ["jar"]
		for filename in os.listdir(path):
			filePath = os.path.join(path, filename)
			if os.path.isfile(filePath):
				if filename.split('.')[-1] in exts:
					os.remove(filePath)
	
	def install(self, **kwargs):
		func = kwargs['func']
		data = None
		if 'data' in kwargs:
			data = kwargs['data']
		mc = None
		forge = None
		
		if func == 'modpack':
			filePath = kwargs['filePath']
			isCurse = kwargs['isCurse']
			
			modpackDir = self.dirRun + "modpack/"
			destFilePath = modpackDir + "modpack.zip"
			
			if isCurse:
				threading.Thread(
					target = installCursePack,
					args = (self.dirRun, modpackDir, filePath, destFilePath, self,)
				).start()
				return
			else:
				if os.path.exists(modpackDir):
					shutil.rmtree(modpackDir)
				os.mkdir(modpackDir)
				
				shutil.move(filePath, destFilePath)
				with zipfile.ZipFile(destFilePath, 'r') as z:
					z.extractall(modpackDir)
				
				# Zip file structure:
				# file
				#  -> mods
				#  -> config
				#  -> ...
				
				if os.path.exists(self.dirRun + "config/"):
					shutil.rmtree(self.dirRun + "config/")
				if os.path.exists(self.dirRun + "mods/"):
					shutil.rmtree(self.dirRun + "mods/")
				
				shutil.move(modpackDir + "config/", self.dirRun + "config/")
				shutil.move(modpackDir + "mods/", self.dirRun + "mods/")
				
				shutil.rmtree(modpackDir)
		elif func == 'server':
			mc = data['version_minecraft']
			forge = data['version_forge']
			self.backup()
			self.cleanRunFiles()
			
			if forge == '':
				url = self.cache.getVersionURL('vanilla', data = {'version': mc})
				data['jar'] = 'minecraft_server.' + mc + '.jar'
				self.updateRunConfig({'jar': data['jar']})
				self.addDownload(url, os.path.join(self.dirRun, data['jar']))
			else:
				# installer
				url = self.cache.getVersionURL('forge', data = {'version': forge, 'mc': mc})
				installerPath = os.path.join(self.dirRun, "forge-installer.jar")
				data['jar'] = "forge-" + forge + "-universal.jar"
				self.updateRunConfig({'jar': data['jar']})
				self.addDownloadFunc(self.downloadAndInstallForge, url = url, installerPath = installerPath)
		
		self.updateRunConfig(data)
	
	def downloadAndInstallForge(self, **kwargs):
		url = kwargs['url']
		installerPath = kwargs['installerPath']
		urllib.urlretrieve(url, installerPath)
		
		env = dict(os.environ)
		FNULL = open(os.devnull, 'w')
		subprocess.call(['java', '-jar', installerPath, '--installServer'], env=env, cwd=self.dirRun, stdout=FNULL, stderr=subprocess.STDOUT)
	
	def setErrors_Modpack(self, errorFiles):
		self.setErrors("modpack", errorFiles)
	
	def appendError_Modpack(self, error):
		if not 'modpack' in self.errors:
			self.setErrors_Modpack([]);
		self.errors['modpack'].append(error)

class Thread(Base.Thread):
	
	def __init__(self, server):
		Base.Thread.__init__(self, server)
	
	def setRunArgs(self):
		self.javaVersions = getJava()
		with open(self.server.dirRun + "run.json", 'r') as outfile:
			runConfig = json.load(outfile)
		self.runArgs = [
			self.javaVersions[0][1] + "/bin/java",
			'-server',
			'-Xms{0}M'.format(runConfig['ram_min']),
			'-Xmx{0}M'.format(runConfig['ram_max']),
			'-XX:MaxPermSize={0}m'.format(runConfig['perm_gen'])
		]
		params = runConfig['other_java_params']
		if not params == '':
			for param in params.split(' '):
				self.runArgs.append(param)
		self.runArgs.extend([
			'-jar', runConfig['jar'],
			'nogui'
		])
		params = runConfig['other_mc_params']
		if not params == '':
			for param in params.split(' '):
				self.runArgs.append(param)
	
	def formatCommand(self, cmd):
		return "/{0}\r\n".format(cmd)
	
	def getJarName(self):
		return self.server.runJar

def installCursePack(dirRun, modpackDir, filePath, destFilePath, server):
	
	server.setDownloading(True)
	
	if os.path.exists(modpackDir):
		shutil.rmtree(modpackDir)
	os.mkdir(modpackDir)
	
	shutil.move(filePath, destFilePath)
	# Zip file structure:
	# file
	#  -> modlist.html
	#  -> manifest.json
	#  -> overrides/
	#      -> mods
	#      -> config
	with zipfile.ZipFile(destFilePath, 'r') as z:
		z.extractall(modpackDir)
	
	modpackMods = modpackDir + 'mods/'
	modpackConfig = modpackDir + 'config/'
	
	overrideMods = modpackDir + 'overrides/mods/'
	if os.path.exists(overrideMods):
		shutil.move(overrideMods, modpackMods)
	else:
		os.mkdir(modpackMods)
	overrideConfig = modpackDir + 'overrides/config/'
	if os.path.exists(overrideConfig):
		shutil.move(overrideConfig, modpackConfig)
	else:
		os.mkdir(modpackConfig)
	
	manifestFile = modpackDir + "manifest.json"
	with open(manifestFile, 'r') as file:
		manifest = json.load(file)
	
	minecraftManifest = manifest['minecraft']
	mc = minecraftManifest['version']
	forge = minecraftManifest['modLoaders'][0]['id']
	forge = mc + '-' + forge.split('-')[1] + '-' + mc
	server.install(func = "server", data = {'version_minecraft': mc, 'version_forge': forge})
	
	urlProject = "http://minecraft.curseforge.com/mc-mods/"

	for fileDict in manifest['files']:
		projectID = fileDict['projectID']
		fileID = fileDict['fileID']
		required = fileDict['required']
		
		projectData = urllib2.urlopen(urlProject + str(projectID))
		urlProjectCorrected = projectData.geturl().split('?')[0]
		projectData.close()
		
		urlFileDownload = urlProjectCorrected + "/files/" + str(fileID) + "/download"
		try:
			fileData = urllib2.urlopen(urlFileDownload)
			fileName = fileData.geturl().split('?')[0].split('/')[-1]
			fileData.close()
			
			urllib.urlretrieve(urlFileDownload, modpackMods + fileName)
		except Exception as e:
			server.appendError_Modpack(str(e) + ' (projectID: ' + str(projectID) + ', fileID: ' + str(fileID) + ')<br />at file url "' + urlFileDownload + '".');
	
	if os.path.exists(dirRun + "config/"):
		shutil.rmtree(dirRun + "config/")
	if os.path.exists(dirRun + "mods/"):
		shutil.rmtree(dirRun + "mods/")
	shutil.move(modpackConfig, dirRun + "config/")
	shutil.move(modpackMods, dirRun + "mods/")
	
	shutil.rmtree(modpackDir)
	
	server.setDownloading(False)

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
