TARGET=architecture
TEXFILES=architecture.tex
DIAGRAMS=diagrams/cd_resources.pdf \
         diagrams/cd_reservation_requests.pdf \
         diagrams/cd_reservations.pdf \
         diagrams/cd_executables.pdf \
         diagrams/dd_architecture.pdf \
         diagrams/dd_architecture_implemented.pdf \
         diagrams/dd_deployment_one.pdf \
         diagrams/dd_deployment_two.pdf \
         diagrams/dd_deployment_multi.pdf \
         diagrams/sd_reservation_request_processing.pdf
         
build: $(TARGET).pdf
	
clean:
	rm -f *.pdf *.aux *.log *.out *.toc *.glg *.glo *.gls *.xdy *.synctex.gz diagrams/*.pdf.crop

DIAGRAMS_CROP=$(DIAGRAMS:.pdf=.pdf.crop)

# Set suffix for PDF files
.SUFFIXES: .pdf

# Pattern rule for cropping PDF files
%.pdf.crop: %.pdf
	pdfcrop $< $@
	cp $@ $<

# Build PDF from tex
$(TARGET).pdf: $(TARGET).toc $(TARGET).gls $(TARGET).tex $(TEXFILES) $(DIAGRAMS_CROP)
	bibtex $(TARGET)
	pdflatex $(TARGET).tex

# Pre-build PDF for table of contents
$(TARGET).toc:
	pdflatex $(TARGET).tex
	
# Build glossaries source from tex	
$(TARGET).gls: $(TARGET).toc
	makeglossaries $(TARGET)
	pdflatex $(TARGET).tex
