#!/bin/bash
#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# Creates a string containing the read-write policy to be used for SFTP User and redirects it into a sftp_policy.json file
echo '{"Version": "2012-10-17","Statement": [{"Sid": "sftpfiletransreadwrite","Effect": "Allow","Action": ["s3:*"],"Resource": ["arn:aws:s3:::*"]}]}' >/tmp/sftp_policy.json
# Creates a string containing the read-only policy to be used for Parser User and redirects it into a parser_policy.json file
echo '{"Version": "2012-10-17","Statement": [{"Sid": "sftpfiletransreadonly","Effect": "Allow","Action": ["s3:GetBucketLocation","s3:GetObject"],"Resource": ["arn:aws:s3:::ran*", "arn:aws:s3:::core*"]}]}' >/tmp/parser_policy.json
IFS=$'\n'
accountCreated=1
while [ $accountCreated -eq 1 ]; do
  if [ ! -f "/var/run/secrets/accesskey" ] || [ ! -f "/var/run/secrets/secretkey" ] || [ ! -f "/var/run/secrets/sftp/parser-bdr-access-key" ] || [ ! -f "/var/run/secrets/sftp/parser-bdr-secret-key" ] || [ ! -f "/var/run/secrets/sftp/sftp-bdr-access-key" ] || [ ! -f "/var/run/secrets/sftp/sftp-bdr-secret-key" ]; then
    echo "At least one of the secret files are NOT available, unable to start Service Accounts Creation"
    sleep ${BDR_SERVICE_ACCOUNT_RETRY_INTERVAL}
  else
    echo "Secret files are available, starting Service Accounts Creation"
    accountCreationCommands=(
         "mc --config-dir \"/tmp\" alias set myalias http://eric-data-object-storage-mn:9000 $MINIO_ROOT_ACCESS_KEY $MINIO_ROOT_SECRET_KEY"
         "mc --config-dir \"/tmp\" admin user add myalias adcSftpParent parentpass"
         "mc --config-dir \"/tmp\" admin user add myalias adcParserParent parentpass"
         "mc --config-dir \"/tmp\" admin policy create myalias sftpfiletransreadwrite /tmp/sftp_policy.json"
         "mc --config-dir \"/tmp\" admin policy create myalias sftpfiletransreadonly /tmp/parser_policy.json"
         "mc --config-dir \"/tmp\" admin policy attach myalias sftpfiletransreadwrite --user=adcSftpParent; mc --config-dir \"/tmp\" admin user info myalias adcSftpParent | grep sftpfiletransreadwrite"
         "mc --config-dir \"/tmp\" admin policy attach myalias sftpfiletransreadonly --user=adcParserParent; mc --config-dir \"/tmp\" admin user info myalias adcParserParent | grep sftpfiletransreadonly"
         "mc --config-dir \"/tmp\" admin user svcacct add myalias adcSftpParent --access-key ${SFTP_ACCESS_KEY} --secret-key ${SFTP_SECRET_KEY};mc --config-dir \"/tmp\" alias set sftpalias http://eric-data-object-storage-mn:9000 ${SFTP_ACCESS_KEY} ${SFTP_SECRET_KEY}"
         "mc --config-dir \"/tmp\" admin user svcacct add myalias adcParserParent --access-key ${PARSER_ACCESS_KEY} --secret-key ${PARSER_SECRET_KEY};mc --config-dir \"/tmp\" alias set parseralias http://eric-data-object-storage-mn:9000 ${PARSER_ACCESS_KEY} ${PARSER_SECRET_KEY}"
    )
    allCommandsSucceeded=0
    for subCommand in "${accountCreationCommands[@]}"; do
      eval $subCommand
      if [ $? -ne 0 ]; then
         allCommandsSucceeded=1;
         break
      fi
    done
    if [ $allCommandsSucceeded -eq 0 ]; then
      echo 'Finished Service Accounts Creation'
      accountCreated=0
    else
      echo 'Retrying Service Accounts Creation'
      sleep ${BDR_SERVICE_ACCOUNT_RETRY_INTERVAL}
    fi
  fi
done