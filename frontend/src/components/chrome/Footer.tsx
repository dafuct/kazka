import styles from './Footer.module.css'

export function Footer() {
  return (
    <footer className={styles.footer}>
      <p className={styles.text}>Казкар — локальний генератор казок на базі Ollama</p>
    </footer>
  )
}
