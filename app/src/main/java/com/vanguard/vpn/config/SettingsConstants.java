package com.vanguard.vpn.config;

public interface SettingsConstants
{
	
	// Geral
	public static final String
		AUTO_CLEAR_LOGS_KEY = "autoClearLogs",
		HIDE_LOG_KEY = "hideLog",
		MODO_DEBUG_KEY = "modeDebug",
		BLOQUEAR_ROOT_KEY = "blockRoot",
	    MODO_NOTURNO_KEY = "modeNight",
		IDIOMA_KEY = "idioma",
		TETHERING_SUBNET = "tetherSubnet",
		MAXIMO_THREADS_KEY = "numberMaxThreadSocks",
		
		FILTER_APPS = "filterApps",
		FILTER_BYPASS_MODE = "filterBypassMode",
		FILTER_APPS_LIST = "filterAppsList",
		
		PROXY_IP_KEY = "proxyRemoto",
		PROXY_PORTA_KEY = "proxyRemotoPorta",
		CUSTOM_PAYLOAD_KEY = "proxyPayload",
		PROXY_USAR_DEFAULT_PAYLOAD = "usarDefaultPayload",
		PROXY_USAR_AUTENTICACAO_KEY = "usarProxyAutenticacao"
	;
	
	public static final String
		CONFIG_PROTEGER_KEY = "protegerConfig",
		CONFIG_MENSAGEM_KEY = "mensagemConfig",
		CONFIG_VALIDADE_KEY = "validadeConfig",
		CONFIG_MENSAGEM_EXPORTAR_KEY = "mensagemConfigExport",
		CONFIG_INPUT_PASSWORD_KEY = "inputPassword"
	;

	// Vpn
	public static final String
	DNSFORWARD_KEY = "dnsForward",
	DNSRESOLVER_KEY = "dnsResolver",
	UDPFORWARD_KEY = "udpForward",
	BYPASS_KEY = "bypassKey",
	UDPRESOLVER_KEY = "udpResolver";

	// SSH
	public static final String
	SERVIDOR_KEY = "sshServer",
	SERVIDOR_PORTA_KEY = "sshPort",
	USUARIO_KEY = "sshUser",
	SENHA_KEY = "sshPass",
	KEYPATH_KEY = "keyPath",
	PORTA_LOCAL_KEY = "sshPortaLocal",
	CHAVE_KEY = "chaveKey",
	NAMESERVER_KEY = "serverNameKey",
	DNS_KEY = "dnsKey",
	PINGER_KEY = "pingerSSH";
	
	public static final String
		PAYLOAD_DEFAULT = "CONNECT [host_port] [protocol][crlf][crlf]";

	// Tunnel Type
	public static final String
		TUNNELTYPE_KEY = "tunnelType",
		TUNNEL_TYPE_SSH_DIRECT = "sshDirect",
		TUNNEL_TYPE_SSH_PROXY = "sshProxy",
	   TUNNEL_TYPE_SSL_PROXY = "sslProxy"
	;
	public static final int
		bTUNNEL_TYPE_SSH_DIRECT = 1,
        bTUNNEL_TYPE_SSH_PROXY = 2,
	    bTUNNEL_TYPE_SSL_PROXY = 3,
	    bTUNNEL_TYPE_SLOWDNS = 4
	;
}
