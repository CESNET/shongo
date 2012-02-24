.SUFFIXES: .tex .pdf

DEPENDS=*.tex ../../bbmri-cerit.cls

default: analysis.pdf

.tex.pdf:
	pdflatex $< && bibtex `basename $< .tex` && pdflatex $< && pdflatex $<
#	pdflatex $< && pdflatex $<

#
# Cleanup
#

clean:
	rm -f *.aux *.log *.out *.nav *.snm *.toc *.blg *.bbl *~ \
		analysis.pdf
