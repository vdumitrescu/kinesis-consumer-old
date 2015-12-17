# kinesis-consumer

A small command-line application for consuming events from one or more AWS Kinesis streams. The events are printed to the console. Useful for debugging data travelling through Kinesis streams.

## Usage

Running the command with `--help` displays the following screen:

```
kinesis-consumer 1.0
Usage: kinesis-consumer [options]

  -a <key> | --accessKey <key>
        Specify the access key to use. Optional, must be accompanied by a secretKey.
  -s <key> | --secretKey <key>
        Specify the secret key to use. Optional, must be accompanied by an accessKey.
  -k <stream name> | --kinesisStream <stream name>
        Specify the name of the Kinesis stream. Mandatory, unless you use -f or -d.
  -r <role arn> | --roleArn <role arn>
        Specify the IAM role to assume before reading from the Kinesis stream. Optional.
  -f <file> | --file <file>
        Specify a configuration file. No other options can be used when this is specified.
  -d <json data> | --data <json data>
        Specify configuration as JSON. No other options can be used when this is specified.
You can specify configuration for a single stream by passing in a streamName and optional accessKey, secretKey, and roleArn.
For multiple streams, you must pass in a configuration file or a JSON structure with the configuration parameters.
  --help
        Prints this usage text
```

To begin streaming from a single stream, just provide a `--streamName`.

The consumer will be using the default AWS credentials to connect to Kinesis and DynamoDB. If there are no default credentials configured, or you simply wish to use other credentials than the default ones, pass in the credentials using `--accessKey` and `--secretKey` parameters.

If the Kinesis stream is accessible through an AWS role which can be assumed by the current user or role, then you can pass the ARN of that role using the `--role` argument. This is particularly useful if the Kinesis stream is in another AWS account.

To consume events from multiple streams at the same time, you must provide configuration either through the `--data` parameter, in JSON format, or using the `--file` parameter, if the configuration JSON is in a file.

The format of the JSON structure:

- `accessKey`: (optional) the access key to use for connecting to AWS.
- `secretKey`: (optional) the secret key to use for connecting to AWS.
- `defaultRole`: (optional) the default role to assume before connecting to Kinesis.
- `streams`: (mandatory) a sequence of configuration object, each containing:
  - `name`: (mandatory) the name of the Kinesis stream.
  - `role`: (optional) the role to assume to consume events from this stream. If missing, it will default to `defaultRole` value.
  
  
## Permissions in AWS

Each AWS resource exists in an AWS account. It is the responsibility of that account owner to assign correct permissions for access to those resources.

This a generic setup and how permissions are used:

1. For each Kinesis stream that is to be read, there must exist a role that has the permission to read from that stream. If there are multiple streams in the same account, then a single role may be used for all.
2. The command-line operator must have an IAM user whose credentials will be passed as arguments, or whose credentials have been set up in the `default` profile, as described in the [AWS CLI setup](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html). The user must have permissions for DynamoDB and STS.
3. Each role defined at (1) above must trust the IAM user at (2).

With these in mind, the kinesis-consumer application will use the user's credentials to call STS and assume the client role(s) for reading from Kinesis.


## Configuration Examples

### Simple: one stream, in the same account, using default credentials

```
{
  "streams": [
    { "name": "calatrava-events-users" }
  ]
}
```

Uses the default AWS credentials to connect to Kinesis and DynamoDB. This will require default credentials to exist, and the IAM user to have permissions to read the stream, and to create a table and records in DynamoDB. The stream must exist in the same account as the IAM user.

### Simple: one stream, in the same account, using provided credentials

```
{
  "accessKey": "BLAH",
  "secretKey": "blahblah",
  "streams": [
    { "name": "calatrava-events-users" }
  ]
}
```

Uses the provided credentials to connect to Kinesis and DynamoDB. The IAM user must have the right permissions. The stream must exist in the same account as the IAM user.

### Simple: one stream, in another account, using default credentials

```
{
  "streams": [
    { "name": "calatrava-events-users", "role": "arn:aws:iam::905260752223:role/calatrava-client" }
  ]
}
```

Uses the default AWS credentials to connect to STS and DynamoDB. This will require default credentials to exist, and the IAM user to have permissions to call STS:AssumeRole and the required DynamoDB permissions. The role in the other account must have permissions to read the stream, which must exist in that account. The role must also allow this IAM user to assume it (trust).

### Intermediate: multiple streams in this account, using default credentials

```
{
  "streams": [
    { "name": "calatrava-events-users" },
    { "name": "calatrava-events-products" },
    { "name": "calatrava-events-skus" },
    { "name": "calatrava-events-orders" }
  ]
}
```

Uses the default AWS credentials to connect to Kinesis and DynamoDB. This will require default credentials to exist, and the IAM user to have permissions to read all streams, and to create a table and records in DynamoDB. The streams must exist in the same account as the IAM user.

### Intermediate: multiple streams in another account, using default credentials

```
{
  "defaultRole": "arn:aws:iam::905260752223:role/calatrava-client"
  "streams": [
    { "name": "calatrava-events-users" },
    { "name": "calatrava-events-products" },
    { "name": "calatrava-events-skus" },
    { "name": "calatrava-events-orders" }
  ]
}
```

Uses the default AWS credentials to connect to STS and DynamoDB. This will require default credentials to exist, and the IAM user to have permissions to call STS:AssumeRole and the required DynamoDB permissions. The role in the other account must have permissions to read all streams, which must exist in that account. The role must also allow this IAM user to assume it (trust).

### Complex: multiple streams in different accounts, using provided credentials

```
{
  "accessKey": "BLAH",
  "secretKey": "blahblah",
  "streams": [
    { "name": "calatrava-events-users", "role": "arn:aws:iam::105260152223:role/calatrava-remote" },
    { "name": "calatrava-events-products", "role": "arn:aws:iam::402602522167:role/calatrava-remote" },
    { "name": "calatrava-events-skus", "role": "arn:aws:iam::260052352923:role/calatrava-remote" },
    { "name": "calatrava-events-orders", "role": "arn:aws:iam::602245923052:role/calatrava-remote" }
  ]
}
```

Uses the provided AWS credentials to connect to STS and DynamoDB. This will require default credentials to exist, and the IAM user to have permissions to call STS:AssumeRole and the required DynamoDB permissions. Every role in the other accounts must have permissions to read the stream, which must exist in that account. Each role must also allow this IAM user to assume it (trust).