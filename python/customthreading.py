#!/usr/bin/env python

from threading import Thread
from Queue import Queue, Empty
import sys

doDownload = True

class ThreadMaster(Thread):
	
	def __init__(self, pool):
		Thread.__init__(self)
		self.daemon = True
		self.running = True
		self.pool = pool
	
	def run(self):
		while self.running:
			pass
	
	def downloadFile(self, url, file):
		if doDownload:
			import urllib
			urllib.urlretrieve(url, file)

	def addDownload(self, url, file):
		self.addDownloadFunc(self.downloadFile, url, file)
	
	def addDownloadFunc(self, func, *args, **kargs):
		if doDownload:
			self.pool.addTask(func, *args, **kargs)

class ThreadPool():
	
	def __init__(self, max_threads):
		self.tasks = Queue()
		self.max_threads = max_threads
		self.running_threads = 0
	
	def addTask(self, func, *args, **kargs):
		self.tasks.put((func, args, kargs))
		if self.running_threads < self.max_threads:
			ThreadWorker(self)
	
	def setWorkerStart(self):
		self.running_threads += 1
	
	def setWorkerFinish(self):
		self.running_threads -= 1

class ThreadWorker(Thread):
	
	def __init__(self, pool):
		Thread.__init__(self)
		self.daemon = True
		self.running = True
		
		self.pool = pool
		self.pool.setWorkerStart()
		
		self.start()
	
	def run(self):
		while self.running:
			try:
				func, args, kargs = self.pool.tasks.get()
			except Empty as e:
				self.running = False
				print(str(e))
				pass
			try:
				#sys.stdout.write("start func\n")
				func(*args, **kargs)
				#sys.stdout.write("end func\n")
			except Exception as e:
				sys.stdout.write("ERROR: " + str(type(e)) + str(e) + "\n")
			finally:
				#sys.stdout.write("done task\n")
				self.pool.tasks.task_done()
		self.pool.setWorkerFinish()
