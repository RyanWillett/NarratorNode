forever stopall
sudo iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 3000
forever -o out.log -e err.log start server.js