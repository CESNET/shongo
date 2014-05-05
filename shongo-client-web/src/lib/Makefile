RESOURCES=../main/resources

build: bootstrap

bootstrap: .FORCE
	cd bootstrap ; grunt dist
	cd bootstrap ; git --no-pager diff package.json less/bootstrap.less less/variables.less > ../bootstrap.diff
	cp bootstrap/dist/css/bootstrap.min.css $(RESOURCES)/css
	cp bootstrap/dist/js/bootstrap.min.js $(RESOURCES)/js
	mkdir -p $(RESOURCES)/fonts
	cp -R font-awesome/css/font-awesome.min.css $(RESOURCES)/css
	cp -R font-awesome/fonts/* $(RESOURCES)/fonts

clean:
	cd bootstrap ; grunt clean

.FORCE:
