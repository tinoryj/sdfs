export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
VERSION=3.7.8
DEBFILE="sdfs_${VERSION}_amd64.deb"
echo $DEBFILE
sudo rm -rf deb/usr/share/sdfs/lib/*
cd ../
mvn package
cd install-packages
sudo cp ../target/lib/b2-2.0.3.jar deb/usr/share/sdfs/lib/
sudo cp ../target/sdfs-${VERSION}-jar-with-dependencies.jar deb/usr/share/sdfs/lib/sdfs.jar
echo

sudo rm *.deb
sudo rm deb/usr/share/sdfs/bin/libfuse.so.2
sudo rm deb/usr/share/sdfs/bin/libulockmgr.so.1
sudo rm deb/usr/share/sdfs/bin/libjavafs.so
sudo cp DEBIAN/libfuse.so.2 deb/usr/share/sdfs/bin/
sudo cp DEBIAN/libulockmgr.so.1 deb/usr/share/sdfs/bin/
sudo cp DEBIAN/libjavafs.so deb/usr/share/sdfs/bin/
sudo cp ../src/readme.txt deb/usr/share/sdfs/

sudo fpm -s dir -t deb -n sdfs -v $VERSION -C deb/ -d fuse --url http://www.opendedup.org -d libxml2 -d libxml2-utils -m sam.silverberg@gmail.com --vendor datishsystems --description "SDFS is an inline deduplication based filesystem" --deb-no-default-config-files
