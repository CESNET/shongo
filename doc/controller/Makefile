TARGET=controller
TEXFILES=code.tex \
         graph.tex \
         chapter_*.tex \
         code_*.txt

build: $(TARGET).pdf
	
clean:
	rm -f *.pdf *.aux *.log *.out *.toc *.glg *.glo *.xdy

# Build PDF from tex
$(TARGET).pdf: $(TARGET).toc $(TARGET).gls $(TARGET).tex $(TEXFILES)
	pdflatex $(TARGET).tex

# Pre-build PDF for table of contents
$(TARGET).toc:
	pdflatex $(TARGET).tex
	
# Build glossaries source from tex	
$(TARGET).gls: glossaries.tex	
	pdflatex $(TARGET).tex
	makeglossaries $(TARGET)
#	pdflatex $(TARGET).tex