RESOURCES=../main/resources

build: bootstrap

bootstrap: .FORCE
	cd bootstrap ; make bootstrap
	cp bootstrap/bootstrap/css/*.min.css $(RESOURCES)/css
	cp bootstrap/bootstrap/js/*.min.js $(RESOURCES)/js
	cp bootstrap/bootstrap/img/* $(RESOURCES)/img

clean:
	cd bootstrap ; make clean

.FORCE:
