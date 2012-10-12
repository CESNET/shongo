TARGET=api
TEXFILES=api.tex
DIAGRAMS=diagrams/cd_api_resources.pdf \
         diagrams/cd_api_reservation_requests.pdf \
         diagrams/cd_api_reservations.pdf \
         diagrams/smd_api_reservation_request.pdf \
         diagrams/sd_reservation_request_allocation.pdf \
         diagrams/sd_reservation_request_set_allocation.pdf \
         diagrams/sd_controller_client_authentication.pdf
         
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
