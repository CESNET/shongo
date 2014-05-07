build: style.css

style.css: .FORCE
	mkdir -p css
	lessc less/lib.less css/lib.css
	lessc less/style.less css/style.css

clean:
	rm -f css/lib.css
	rm -f css/style.css

.FORCE:
