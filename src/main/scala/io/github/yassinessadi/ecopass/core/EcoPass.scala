
package io.github.yassinessadi.ecopass

import io.github.yassinessadi.ecopass.indexing.{
  EcoPassIndex,
  EcoPassIndexGenerator
}

object EcoPass {

  /**
   * Generate an index using default framing:
   * 2-byte header and 2-byte alignment.
   */
  def generateIndex(
      sourcePath: String,
      indexPath: String
  ): EcoPassIndex = {

    EcoPassIndexGenerator.generate(
      sourcePath,
      indexPath,
      headerSize = 2,
      alignment = 2
    )
  }

  /**
   * Generate an index using custom fixed framing.
   */
  def generateIndex(
      sourcePath: String,
      indexPath: String,
      headerSize: Int,
      alignment: Int
  ): EcoPassIndex = {

    EcoPassIndexGenerator.generate(
      sourcePath,
      indexPath,
      headerSize,
      alignment
    )
  }
}
