TARGET=api
TEXFILES=api.tex
         
build: $(TARGET).pdf
	
clean:
	rm -f *.pdf *.aux *.log *.out *.toc *.glg *.glo *.gls *.xdy *.synctex.gz

# Build PDF from tex
$(TARGET).pdf: $(TARGET).toc $(TARGET).gls $(TARGET).tex $(TEXFILES)
	bibtex $(TARGET)
	pdflatex $(TARGET).tex

# Pre-build PDF for table of contents
$(TARGET).toc:
	pdflatex $(TARGET).tex
	
# Build glossaries source from tex	
$(TARGET).gls: $(TARGET).toc
	makeglossaries $(TARGET)
	pdflatex $(TARGET).tex
