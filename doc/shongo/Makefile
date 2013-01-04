#
# Makefile for Shongo documentation
#

build: build.shongo_api build.shongo_connectors

clean: clean.shongo_api clean.shongo_connectors

build.shongo_api:
	cd api && make
	cp api/api.pdf shongo_api.pdf

clean.shongo_api:
	cd api && make clean
	rm -f shongo_api.pdf

build.shongo_connectors:
	cd connectors && make
	cp connectors/connectors.pdf shongo_connectors.pdf

clean.shongo_connectors:
	cd connectors && make clean
	rm -f shongo_connectors.pdf
