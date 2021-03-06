import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let myHost
let myPort

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))
  
cli
  .mode('connect <username> <host> <port>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    myHost = args.host
    myPort = args.port
    server = connect({ host: myHost, port: myPort }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })
   
    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')

    if (command === 'disconnect') {
      cli
        .delimiter(cli.chalk['red']('disconnect>'))
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      cli
        .delimiter(cli.chalk['blue']('echo>'))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'users') {
      cli
        .delimiter(cli.chalk['magenta']('users>'))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      cli
        .delimiter(cli.chalk['cyan']('broadcast>'))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === '@username') {
      cli
        .delimiter(cli.chalk['white']('username>'))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else {
      cli
        .delimiter(cli.chalk['gray']('>'))
     server.write(new Message({ username, command, contents }).toJSON() + '\n')
    }

    callback()
  })

  