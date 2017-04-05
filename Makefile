#
# Makefile for Java OpenGL based on LWJGL
#   rdb
#   08/10/2014 Created from JOGL version except this version does not
#              depend on the user's CLASSPATH already being set to include
#              the LWJGL jar files
#   08/18/2014 
#   09/06/2014 Changes to provide alternate locations for libraries and
#              the Main class name with environment variable override 
#              for grading.
#   11/27/2016 Changed to look for lwjgl directory instead of lwjgl3
#   12/22/2016 Modified to more easily mesh with distribution of lwjgl3.1.1
#
# Labeled entries:
#     all:    build, then run
#     build:  compile all java files
#     run:    run the java class designated as MAIN
#     new:    delete all class files, build, run
#     clean:  delete all class files
#
#---------------------------------------------------------------------
#************************************************************************
#
#     Edit next line to specify the name of your main class
#------------------------------------------------------------------------
MAIN = P3

#------------------------------------------------------------------------
#
#     Edit the next line to specify the location of your home directory
#     for CS770. On department workstations, it's likely that you have a
#     directory for cs770 in your home directory. If so, you don't need to
#     edit this line -- but you may not want to follow that convention on
#     your home machine. your CS770 home directory must have subdirectories:
#        jars:  that must have "lwjgl770.jar" and any other jar files that 
#                   may be needed for using lwjgl3.
#        lib/lwjgl: that must have all the native (C or C++) support code
#                   needed by lwjgl3. 
#
#     NO MATTER WHAT YOU SPECIFY BELOW, YOU MUST LEAVE THE LEFT SIDE OF 
#         THE ASSIGNMENT UNCHANGED:
#
#     HOME770 ?=  
#
#     If you don't, your program will not run correctly when we test it! 
#
#------------------------------------

HOME770 ?= /Users/Nithin/Documents/HOME770

#------------------------------------

# set up
JARDIR=$(HOME770)/jars
LWJGL_LIBDIR=$(HOME770)/lib/lwjgl-native

#--------------------------------

JARS = $(wildcard $(JARDIR)/*jar) 
JVMFLAGS = 

#---------- System dependencies ---------------------------------------
# The lwjgl native libraries are in 
#    $(CS770LWJGL_LIBS) 
# They are differently named for different OS's 
# *.dylib (for mac os x) or *.so (for linux) or *dll (for windows
# 
# But, we need only specify the directory containing the libraries
OS := $(shell uname)
ifeq ($(OS),Darwin)
    JVMFLAGS +=  -XstartOnFirstThread
    #JVMFLAGS +=  -Djava.awt.headless=true # only need if using AWT features
endif

LIBFLAGS = -Djava.library.path=$(LWJGL_LIBDIR)

#----------- get all jar files needed in the classpath ------------------
#
# Next line gets a space separated list of jar files; we need it to 
#    be : -separated for the -cp switch.

# Next 2 lines get a space into "space"
space :=
space +=
# This line converts all spaces to :
JARS := $(subst $(space),:,$(JARS))

# proc:none turns off annotation processing. We use it to avoid a
#   warning message from compiling in the context of the gluegen 
#   annotation processor, which may be compiled for an older Java version.
JCFLAGS = -proc:none

# Now add in the class path and include .
#if only want lwjgl classpath:
JCFLAGS += -cp .:$(JARS)
#if want existing classpath also: 
#JCFLAGS += -cp .:$(JARS):$(CLASSPATH)

# Library options

#---------- Application info ------------------------------------------

SRCS = $(wildcard *java)

# for every .java input, need to produce a .class
OBJS = $(SRCS:.java=.class) 

#------------------- dependencies/actions ------------------------
# dependency: need1 need2 ...  
#         action(s)
#
.PHONY: clean 

all:	build run

build: compile $(MAIN)

compile: $(OBJS) 

new:	clean build run

%.class : %.java 
	javac $(JCFLAGS) $(SRCS)

$(MAIN): $(OBJS) $(COBJS)

run:
	java $(JVMFLAGS) $(LIBFLAGS) -cp $(JARS) $(MAIN) $(ARGS)

clean:
	rm -f *.class 
