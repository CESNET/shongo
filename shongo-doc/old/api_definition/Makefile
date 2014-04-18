.SUFFIXES: .tex .pdf

DEPENDS=*.tex ../../bbmri-cerit.cls 

default: API.pdf

.tex.pdf:
	#pdflatex $< && bibtex `basename $< .tex` && pdflatex $< && pdflatex $<
	pdflatex $< && pdflatex $<

#
# Cleanup
#

clean:
	rm -f *.aux *.log *.out *.nav *.snm *.toc *.tex~ API.pdf 
