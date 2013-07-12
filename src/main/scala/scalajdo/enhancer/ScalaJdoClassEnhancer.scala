package scalajdo.enhancer

import org.datanucleus.metadata.ClassMetaData
import org.datanucleus.ClassLoaderResolver
import org.datanucleus.metadata.MetaDataManager

class ScalajdoClassEnhancer(
    val cmd: ClassMetaData,
    val clr: ClassLoaderResolver,
    val mmgr: MetaDataManager,
    val classBytes: Array[Byte]) {
  
}