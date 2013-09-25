RESOURCES=../main/resources

build: bootstrap

bootstrap: .FORCE
	cd bootstrap ; make bootstrap
	cd bootstrap ; git diff > ../bootstrap.diff
	cp bootstrap/bootstrap/css/*.min.css $(RESOURCES)/css
	cp bootstrap/bootstrap/js/*.min.js $(RESOURCES)/js
	cp -R font-awesome/font $(RESOURCES)/

clean:
	cd bootstrap ; make clean

.FORCE:
