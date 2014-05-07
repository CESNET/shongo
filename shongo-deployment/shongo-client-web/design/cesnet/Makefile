build: style.css

style.css: .FORCE
	lessc less/style.less css/style.css
	#yui-compressor css/style.css > css/style.min.css

clean:
	rm -f css/style.css
	rm -f css/style.min.css

.FORCE:
