set -e

if [[ "$1" == "" ]]; then
	echo "run.sh <ex#> <module> <class> [...]"
fi

clsdir=classes
objsdk=openbusjava
id=$1

if (( $id > 4 )); then
	java="java -Djava.endorsed.dirs=$objsdk/lib"
else
	java=java
fi

classpath=.
for (( i = 1; i <= $id; ++i )); do
	prev=`printf %02d $i`
	classpath="$clsdir/ex$prev:$classpath"
done

$java -cp $classpath tecgraf.openbus.demo.$2.$3 ${@:4:${#@}}
