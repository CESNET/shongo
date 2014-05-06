build: style.css

style.css: .FORCE
	lessc src/style.less style.css
	#yui-compressor style.css > style.min.css

clean:
	rm -f style.css
	rm -f style.min.css

.FORCE:
