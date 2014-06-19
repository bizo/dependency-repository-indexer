package depsearch.db.mongo


class VersionParser {
  val majorMinorPatch = """(\d+)\.(\d+)\.(\d+)(.*)""".r
  val majorMinor = """(\d+)\.(\d+)(.*)""".r
  
  def sortVersion(s: String): String = {
    s match {
      case majorMinorPatch(major, minor, patch, rest) =>
        pad(major) + "." + pad(minor) + "." + pad(patch) + rest
      case majorMinor(major, minor, rest) =>
        pad(major) + "." + pad(minor) + rest
      case _ => s
    }
  }
  
  private def pad(s: String): String = "%04d".format(s.toInt)
}