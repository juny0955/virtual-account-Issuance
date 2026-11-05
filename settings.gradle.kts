rootProject.name = "vai"

include(":persistence")
project(":persistence").projectDir = file("modules/persistence")

include(":api")
project(":api").projectDir = file("modules/api")

include(":batch")
project(":batch").projectDir = file("modules/batch")
