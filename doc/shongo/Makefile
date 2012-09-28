#
# Makefile for Shongo documentation
#

build: build.shongo_api
	
clean: clean.shongo_api

build.shongo_api: 
	cd api && make
	cp api/api.pdf shongo_api.pdf

clean.shongo_api:
	cd api && make clean
	rm -f shongo_api.pdf

