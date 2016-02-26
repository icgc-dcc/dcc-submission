/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.loader.db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.model.PostgresqlCredentials;

@Slf4j
@RequiredArgsConstructor
public class SqlFileExecutor {

  /**
   * Constants.
   */
  private static final String SQL_CLIENT_NAME = "psql";
  private static final String POSTGRESQL_PASSWORD_ENV_VARIABLE = "PGPASSWORD";
  private static final String STORED_FUNCTIONS_FILE = "src/main/sql/create-functions.sql";

  @NonNull
  private final PostgresqlCredentials postgresCredentials;

  public void createStoredFunctions() {
    executeFile(STORED_FUNCTIONS_FILE);
  }

  public void executeFile(String sqlFile) {
    if (hasSqlClient() == false) {
      log.warn("Failed to SQL client {}. Execute the script {} manually.", SQL_CLIENT_NAME, sqlFile);
      return;
    }

    val clientPath = resolveClientPath();
    log.debug("Client path: {}", clientPath);

    String[] command = createExecCommand(postgresCredentials, clientPath, sqlFile);

    val commandOutput = execute(Optional.of(postgresCredentials.getPassword()), command);
    log.info(commandOutput);
  }

  @SneakyThrows
  private static String execute(Optional<String> postgresPassword, String... command) {
    val processBuilder = createProcessBuilder();
    if (postgresPassword.isPresent()) {
      setPassword(processBuilder, postgresPassword.get());
    }

    val process = processBuilder
        .command(command)
        .start();

    return executeProcess(process).trim();
  }

  private static ProcessBuilder createProcessBuilder() {
    val processBuilder = new ProcessBuilder();
    processBuilder.redirectErrorStream(true);
    val environment = processBuilder.environment();
    environment.put("BASH_ENV", "/etc/profile");

    return processBuilder;
  }

  private static void setPassword(ProcessBuilder processBuilder, String postgresPassword) {
    processBuilder
        .environment()
        .put(POSTGRESQL_PASSWORD_ENV_VARIABLE, postgresPassword);
  }

  private static String[] createExecCommand(PostgresqlCredentials postgresCredentials, String clientPath, String sqlFile) {
    log.debug("{}", postgresCredentials);

    return new String[] { clientPath,
        "-h", postgresCredentials.getHost(),
        "-p", postgresCredentials.getPort(),
        "-U", postgresCredentials.getDbName(),
        "-f", sqlFile };
  }

  @SneakyThrows
  private static String executeProcess(Process process) {
    val processOutput = new StringBuilder();
    String line = null;
    val processInputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
    while ((line = processInputStream.readLine()) != null) {
      processOutput.append(line);
      processOutput.append(System.lineSeparator());
    }

    return processOutput.toString();
  }

  private static boolean hasSqlClient() {
    return resolveClientPath() != null;
  }

  @SneakyThrows
  private static String resolveClientPath() {
    val output = execute(Optional.empty(), "/bin/bash", "-c", "/usr/bin/which psql");
    log.debug("Resolve client path output: {}", output);

    return output.contains(SQL_CLIENT_NAME) ? output : null;
  }

}
