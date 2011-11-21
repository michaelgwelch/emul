import sys, os.path

from twisted.application import internet, service
from twisted.internet import reactor
from twisted.web import static, server
from twisted.web.resource import Resource

class MyResource(Resource):
    def render_GET(self, request):
        return "<html>Hello, world!</html>"
    
root = MyResource()
v9t9 = static.File(os.path.split(os.path.realpath(__file__))[0])

root.putChild("v9t9", v9t9)

application = service.Application('web')
site = server.Site(root)
sc = service.IServiceCollection(application)
i = internet.TCPServer(8080, site)
i.setServiceParent(sc)

reactor.listenTCP(8080, server.Site(root))
reactor.run()

