TARGET=connectors

build: $(TARGET).pdf

clean:
	rm -f *.pdf *.aux *.log *.out *.toc *.glg *.glo *.gls *.xdy *.synctex.gz

# Set suffix for PDF files
.SUFFIXES: .pdf

# Build PDF from tex
$(TARGET).pdf: $(TARGET).tex
	pdflatex $(TARGET).tex && pdflatex $(TARGET).tex
