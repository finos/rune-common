package com.regnosys.rosetta.common.transform;

public enum TransformType {
  TRANSLATE("translate", "TBD"),
  PROJECTION("projection", "projections.%sProjectionTabulator"),
  REPORT("regulatory-reporting", "reports.%sReportTabulator");

  private final String resourcePath;
  private final String tabulatorName;

  TransformType(String resourcePath, String tabulatorName) {
    this.resourcePath = resourcePath;
    this.tabulatorName = tabulatorName;
  }

  /**
   * This is the path in either github, or local file system where the transform files are located.
   *
   * @return the path to the transform
   */
  public String getResourcePath() {
    return resourcePath;
  }

  public String getTabulatorName(String functionName) {
    return tabulatorName.format(tabulatorName, functionName);
  }
}
