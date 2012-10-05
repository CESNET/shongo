TARGET=api
TEXFILES=api.tex
         
build: $(TARGET).pdf
	
clean:
	rm -f *.pdf *.aux *.log *.out *.toc *.glg *.glo *.gls *.xdy *.synctex.gz

# Build PDF from tex
$(TARGET).pdf: $(TARGET).toc $(TARGET).gls $(TARGET).tex $(TEXFILES)
	bibtex $(TARGET)
	pdfcrop diagrams/cd_api_resources.pdf diagrams/cd_api_resources.pdf
	pdfcrop diagrams/cd_api_reservation_requests.pdf diagrams/cd_api_reservation_requests.pdf
	pdflatex $(TARGET).tex

# Pre-build PDF for table of contents
$(TARGET).toc:
	pdflatex $(TARGET).tex
	
# Build glossaries source from tex	
$(TARGET).gls: $(TARGET).toc
	makeglossaries $(TARGET)
	pdflatex $(TARGET).tex
