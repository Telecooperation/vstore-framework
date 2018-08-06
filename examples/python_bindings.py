from py4j.java_gateway import JavaGateway
from py4j.java_gateway import java_import
import time

# connect to the JVM
gateway = JavaGateway()
jvm = gateway.jvm
vstore = gateway.entry_point
java_import(jvm, 'vstore.framework.*')
java_import(jvm, 'vstore.framework.context.*')
java_import(jvm, 'vstore.framework.context.types.*')
java_import(jvm, 'vstore.framework.context.types.location.*')
java_import(jvm, 'vstore.framework.config.*')

# Download configuration file
confmgr = vstore.getConfigManager()
confmgr.download(True)
print("Configuration download done")

# Create a fake context
ctxDescription = jvm.ContextDescription()

latLng = jvm.VLatLng(49.877684, 8.654256)
locCtx = jvm.VLocation(latLng, 2.0, int(time.time())*1000, "")

ctxDescription.setLocationContext(locCtx)

vstore.provideContext(ctxDescription)
vstore.persistContext(True)

# Print number of storage nodes
print ("{}{}".format("Node Count: ", vstore.getNodeManager().getNodeCount()))
