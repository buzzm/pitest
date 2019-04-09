/*
 * Copyright 2011 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.report.json;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.util.ResultOutputStrategy;
import org.pitest.util.Unchecked;


public class JSONReportListener implements MutationResultListener {

  private final Writer out;
  private final boolean fullMutationMatrix;

  // Need class scope to track across potentially multiple calls to
  // the handleMutationResult method:
  private boolean oneDone = false;

    /**
     *  Emit a JSON report.
     *  Format is standalone array as COMMA-CRJSON, e.g.
     *  [{data},
     *  {data},
     *  {data}]
     *
     *  This format permits accurate use of wc for count and easy grepping for
     *  material at the same time yielding the rest of the material.
     *  Readability is "reasonable."
     */
  public JSONReportListener(final ResultOutputStrategy outputStrategy, boolean fullMutationMatrix) {
    this(outputStrategy.createWriterForFile("mutations.json"), fullMutationMatrix);
  }

  public JSONReportListener(final Writer out, boolean fullMutationMatrix) {
    this.out = out;
    this.fullMutationMatrix = fullMutationMatrix;
  }


  private void writeResult(final ClassMutationResults metaData) {
    for (final MutationResult mutation : metaData.getMutations()) {
      if (oneDone) {
        write(",\n");
      }
      writeOneResult(mutation);
      oneDone = true;
    }
  }


  private void writeOneResult(final MutationResult result) {
      final MutationDetails details = result.getDetails();

      write("{"); 
      write("\"detected\":" + result.getStatus().isDetected() + ",");
      write("\"status\":\"" + result.getStatus() + "\",");
      write("\"numberOfTestsRun\":" + result.getNumberOfTestsRun() + ",");
      write("\"sourcefile\":\"" + details.getFilename() + "\",");
      write("\"mutatedClass\":\"" + details.getClassName().asJavaName() + "\",");
      write("\"mutatedMethod\":\"" + details.getMethod().name() + "\",");
      write("\"methodDescription\":\"" + details.getId().getLocation().getMethodDesc() + "\",");
      write("\"lineNumber\":" + details.getLineNumber() + ",");
      write("\"mutator\":\"" + details.getMutator() + "\",");
      write("\"index\":" + details.getFirstIndex() + ",");
      write("\"block\":" + details.getBlock() + ",");

      if (!fullMutationMatrix && result.getKillingTest().isPresent()) {
          write( "\"killingTest\":\"" + result.getKillingTest().get() + "\",");
      }

      writeAList("killingTests", result.getKillingTests()); 
      writeAList("succeedingTests", result.getSucceedingTests());

      write("\"description\":\"" + details.getDescription() + "\"");

      write("}");
  }


  private void writeAList(final String tag, List<String> items) {
      if (fullMutationMatrix && items != null && items.size() > 0) {
          write( "\"" + tag + "\": [");
          boolean z2one = false;
          for (String item : items) {
              if (z2one) {
                  write(",");
              }
              write("\"" + item + "\"");
              z2one = true;
          }
          write("],");
      }
  }

  private void write(final String value) {
    try {
      this.out.write(value);
    } catch (final IOException e) {
      throw Unchecked.translateCheckedException(e);
    }
  }


  @Override
  public void runStart() {
      write("[");
  }

  // Careful!  as a listener, this result call can be hit MULTIPLE times!  The
  // engine will control how many mutations are "collected" before deciding to
  // pass the "batch" to this listener.
  @Override
  public void handleMutationResult(final ClassMutationResults metaData) {
    writeResult(metaData);
  }


  @Override
  public void runEnd() {
    try {
      write("]\n");
      this.out.close();
    } catch (final IOException e) {
      throw Unchecked.translateCheckedException(e);
    }
  }

}
