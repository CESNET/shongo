TARGET=scheduler

build: $(TARGET)
	
clean:
	rm -f *.pdf *.aux *.log *.out *.toc
	
GRAPH=$(wildcard graph/*.dot) \
    $(wildcard graph/*.twopi) \
    $(wildcard graph/*.circo)
    
GRAPH_BUILD=$(GRAPH:.dot=.tex.build) \
    $(GRAPH:.twopi=.tex.build) \
    $(GRAPH:.circo=.tex.build)

$(TARGET): ${GRAPH_BUILD}
	pdflatex $(TARGET).tex

%.tex.build : %.dot
	dot2tex --figonly $< > $(shell echo "$@" | sed s/.build//)

%.tex.build : %.twopi
	dot2tex --figonly --prog twopi $< > $(shell echo "$@" | sed s/.build//)
        	
%.tex.build : %.circo
	dot2tex --figonly --prog circo $< > $(shell echo "$@" | sed s/.build//)
                	