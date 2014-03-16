set -e

if [[ "$1" == "" ]]; then
	echo "build.sh <ex#>"
fi

javac=javac
srcdir=../src
idl2jdir=idl2java
clsdir=classes
objsdk=openbusjava
id=$1

classpath=.

if (( $id > 4 )); then
	for jar in $objsdk/lib/*.jar; do
		classpath="$classpath:$jar"
	done
	idlc="java -cp $classpath org.jacorb.idl.parser -generate_helper portable -d"
else
	idlc="idlj -fallTIE -td"
fi

exid=`printf %02d $id`
if [[ -d $srcdir/ex$exid ]]; then
	if [[ ! -d $clsdir/ex$exid ]]; then mkdir -p $clsdir/ex$exid; fi
	if [[ -d $srcdir/ex$exid/idl ]]; then
		if [[ ! -d $idl2jdir/ex$exid ]]; then mkdir -p $idl2jdir/ex$exid; fi
		for idl in $srcdir/ex$exid/idl/*.idl; do
			$idlc $idl2jdir/ex$exid $idl
		done
	elif (( $id == 5 )); then
		if [[ ! -d $idl2jdir/ex$exid ]]; then mkdir -p $idl2jdir/ex$exid; fi
		for idl in $srcdir/ex04/idl/*.idl; do
			$idlc $idl2jdir/ex$exid $idl
		done
	fi
	if [[ -d $srcdir/ex$exid/java ]]; then
		sourcepath="$srcdir/ex$exid/java:$idl2jdir/ex$exid"
		for (( i = $id - 1; i > 0; --i )); do
			prev=`printf %02d $i`
			classpath="$classpath:$clsdir/ex$prev"
			sourcepath="$sourcepath:$idl2jdir/ex$prev"
		done
		for demo in $srcdir/ex$exid/java/tecgraf/openbus/demo/*; do
			for system in Server Application; do
				if [[ -e $demo/$system.java ]]; then
					$javac -d $clsdir/ex$exid -cp $classpath -sourcepath $sourcepath \
					       $demo/$system.java
				fi
			done
		done
	fi
fi
