set container=testcontainer

REM Remove old container
docker rmi -f test 

REM Build
docker build -t test .

REM Clean
docker system prune --volumes -f

REM Stop old
docker rm -f %container%

REM RUN
docker run -d -p 8080:8080 --name %container% -v "H:/OneDrive/workspace/EvergoreProtocolCollector/database:/database" test

docker run -p 8080:8080 --name %container% -v "H:\workspace\EvergoreProtocolCollector\dockerDB:/database" test


REM Show logs
docker logs -f %container%
