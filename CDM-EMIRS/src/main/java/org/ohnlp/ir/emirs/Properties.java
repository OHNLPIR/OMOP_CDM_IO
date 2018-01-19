package org.ohnlp.ir.emirs;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class Properties {

    private ElasticsearchSettings es;
    private Uima uima;

    private int docsPerPageDocView;
    private int docsPerPagePatientView;
    private int patientsPerPage;

    public ElasticsearchSettings getEs() {
        return es;
    }

    public Uima getUima() {
        return uima;
    }

    public void setUima(Uima uima) {
        this.uima = uima;
    }

    public void setEs(ElasticsearchSettings es) {
        this.es = es;
    }

    public static class ElasticsearchSettings {
        @NotBlank
        private String host;
        @NotBlank
        private int port;
        @NotBlank
        private String indexName;
        @NotBlank
        private String clusterName;
        @NotBlank
        private int httpport;

        public int getHttpport() {
            return httpport;
        }

        public void setHttpport(int httpport) {
            this.httpport = httpport;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }
    }

    public static class Uima {
        @NotBlank
        private String host;
        @NotBlank
        private int port;
        @NotBlank
        private String queue;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }
    }

    public int getDocsPerPageDocView() {
        return docsPerPageDocView;
    }

    public void setDocsPerPageDocView(int docsPerPageDocView) {
        this.docsPerPageDocView = docsPerPageDocView;
    }

    public int getDocsPerPagePatientView() {
        return docsPerPagePatientView;
    }

    public void setDocsPerPagePatientView(int docsPerPagePatientView) {
        this.docsPerPagePatientView = docsPerPagePatientView;
    }

    public int getPatientsPerPage() {
        return patientsPerPage;
    }

    public void setPatientsPerPage(int patientsPerPage) {
        this.patientsPerPage = patientsPerPage;
    }
}
